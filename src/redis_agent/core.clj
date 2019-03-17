(ns redis-agent.core
  (:require [taoensso.carmine :as car :refer [wcar]]
            [redis-agent.utils :refer [finalize!]]
            [redis-agent.map :refer [->RedisMap]]))

(defn global-map [uri]
  (agent (->RedisMap {:pool {} :spec {:uri uri}} {})))

(defn mutate [r f & args]
  (apply send r f args)
  (send r finalize!))

(defn mutate-off [r f & args]
  (apply send-off r f args)
  (send-off r finalize!))
