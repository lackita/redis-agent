(ns redis-agent.core
  (:require [taoensso.carmine :as car :refer [wcar]]))

(defn build-connection [uri]
  {:pool {} :spec {:uri uri}})

(defprotocol Insertable
  (put [_ k v]))

(deftype RedisMap [connection]
  Insertable
  (put [_ k v]
    (wcar connection
          (car/hset :-type k (cond
                               (number? v) "number"
                               :else       nil))
          (car/set k v)))
  clojure.lang.ILookup
  (valAt [_ k]
    (let [[t v] (wcar connection
                      (car/hget :-type k)
                      (car/get k))]
      (if (= t "number")
        (read-string v)
        v)))
  clojure.lang.IFn
  (invoke [m k] (get m k)))

(defn global-map [connection]
  (RedisMap. connection))
