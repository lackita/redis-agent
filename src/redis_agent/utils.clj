(ns redis-agent.utils)

(defprotocol Persistable
  (finalize! [this]))
