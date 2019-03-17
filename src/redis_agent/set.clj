(ns redis-agent.set
  (:require [redis-agent.utils :refer [Persistable]]))

(deftype RedisSet [connection key meta])
