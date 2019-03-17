(ns redis-agent.test-support
  (:require [clojure.test :refer :all]
            [redis-agent.core :as r]))

(defn mutate [a & args]
  (apply r/mutate-off a args)
  (await-for 100 a)
  (is (nil? (agent-error a)))
  (await a))
