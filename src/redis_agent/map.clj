(ns redis-agent.map
  (:require [taoensso.carmine :as car :refer [wcar]]
            [redis-agent.utils :refer [Persistable]]))

(defn simple-type [v]
  (cond
    (string? v) ::string
    (number? v) ::number
    (map? v)    ::map
    :else       nil))
(derive ::string ::scalar)
(derive ::number ::scalar)

(defmulti finalize-commands (fn [_ v] (simple-type v)))
(defmethod finalize-commands ::scalar [k v]
  `[(car/hset :-type ~k (simple-type ~v))
    (car/set ~k ~v)])
(defmethod finalize-commands ::map [k v]
  `[(car/hset :-type ~k (simple-type ~v))
    (car/del ~k)
    ~@(map (fn [[hk hv]] `(car/hset ~k ~hk ~hv)) v)]  )
(defmethod finalize-commands nil [k _]
  `[(car/hset :-type ~k nil)
    (car/del ~k)])

(declare ->RedisMap ->RedisHashMap)
(defn redis-map-scan [original current-scan]
  (let [[n is] (wcar (.connection original) (car/scan current-scan))]
    (->RedisMap (.connection original)
                (assoc (.meta original) :scan {:next n :items is}))))

(defmulti reconstitute-type (comp first list))
(defmethod reconstitute-type ::string [_ v] v)
(defmethod reconstitute-type ::number [_ v] (read-string v))
(defmethod reconstitute-type ::map [_ c k] (->RedisHashMap c k {}))
(defmethod reconstitute-type nil [_ v] nil)

(deftype RedisHashMap [connection key meta]
  clojure.lang.ILookup
  (valAt [_ k]
    (let [overlay (:overlay meta)]
      (if (contains? overlay k)
        (overlay k)
        (let [t (keyword (wcar connection (car/hget (str "-hash-type-" key) k)))]
          (reconstitute-type t (wcar connection (car/hget key k)))))))

  clojure.lang.IFn
  (invoke [this k] (.valAt this k))

  clojure.lang.IPersistentMap
  (assoc [_ k v]
    (->RedisHashMap connection key (assoc-in meta [:overlay k] v)))
  (without [this k]
    (->RedisHashMap connection key (assoc-in meta [:overlay k] nil)))

  clojure.lang.ISeq
  (seq [this] (seq (into {} (map (fn [[k v]] [k (reconstitute-type (keyword (wcar connection (car/hget (str "-hash-type-" key) k))) v)])
                   (apply array-map (wcar connection (car/hgetall key)))))))
  (iterator [this] (.iterator (seq this)))

  Persistable
  (finalize! [_]
    (eval `(wcar ~connection
                 ~@(mapcat (fn [[k v]] `[(car/hset ~(str "-hash-type-" key)
                                                   ~k ~(simple-type v))
                                         ~(if v
                                            `(car/hset ~key ~k ~v)
                                            `(car/hdel ~key ~k))])
                           (meta :overlay))))
    (->RedisHashMap connection key (assoc meta :overlay {}))))

(deftype RedisMap [connection meta]
  clojure.lang.ILookup
  (valAt [_ k]
    (let [overlay (:overlay meta)]
      (if (contains? overlay k)
        (overlay k)
        (let [t (keyword (wcar connection (car/hget :-type k)))]
          (if (= t ::map)
            (reconstitute-type t connection k)
            (reconstitute-type t (wcar connection (car/get k))))))))

  clojure.lang.IFn
  (invoke [this k] (.valAt this k))

  clojure.lang.IPersistentMap
  (assoc [_ k v]
    (->RedisMap connection (assoc-in meta [:overlay k] v)))
  (without [this k]
    (->RedisMap connection (assoc-in meta [:overlay k] nil)))

  clojure.lang.ISeq
  (seq [this] (seq (into {} (concat (map (fn [k] [(keyword k) (get this k)])
                                         (wcar connection (car/keys "[^-]*")))
                                    (meta :overlay)))))
  (iterator [this] (.iterator (seq this)))
  (first [this]
    (let [scan (meta :scan)]
      (cond
        (not (empty? (meta :overlay))) (first (meta :overlay))
        (nil? (scan :next)) (first (redis-map-scan connection 0))
        (and (empty? (scan :items)) (zero? (scan :next))) nil
        (empty? (scan :items)) (first (redis-map-scan connection (scan :next)))
        :else (first (scan :items)))))
  (next [_]
    (let [scan (meta :scan)]
      (cond
        (nil? (scan :next)) (next (redis-map-scan connection 0))
        (and (empty? (scan :items)) (zero? (scan :next))) nil
        (empty? (scan :items)) (next (redis-map-scan connection (scan :next)))
        :else (->RedisMap connection (assoc-in meta [:scan :items] (next (scan :items)))))))

  Persistable
  (finalize! [_]
    (eval `(wcar ~connection
                 ~@(mapcat (fn [[k v]] (finalize-commands k v))
                           (meta :overlay))))
    (->RedisMap connection (assoc meta :overlay {}))))
