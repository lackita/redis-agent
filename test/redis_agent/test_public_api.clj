(ns redis-agent.test-public-api
  (:require [clojure.test :refer :all]
            [redis-agent.core :as r]
            [redis-agent.test-support :refer [mutate]]
            [taoensso.carmine :as car :refer [wcar]]))

;; Pending tests are quoted. Anything pending represents work required
;; to release version 1.0.0

;; A word of caution, some of the operations this library provides are
;; fairly broad, so running the tests in this file can be fairly
;; destructive to what's in an existing redis server. I've
;; intentionally used a non-standard port so somebody doesn't blindly
;; run the tests and blow everything away. If you're interested in
;; running these tests, I'd suggest you set up a second redis server
;; on your machine explicitly for testing.

(defonce g (r/global-map "redis://localhost:6380/"))

;; To simulate an immutable map within the sent function, redis-agent
;; provides the functions `mutate` and `mutate-off`, which should be
;; used the same way `send` and `send-off` are used. To better
;; understand what's going on here, see `concurrency-behavior` further
;; down.
(deftest simple-interactions
  (testing "when a new value is associated, that's what becomes available"
    (mutate g assoc :test "x")
    (is (= (get @g :test) "x")))
  (testing "type is retained"
    (mutate g assoc :test 1)
    (is (= (get @g :test) 1))

    (mutate g assoc :test nil)
    (is (nil? (:test @g))))
  (testing "you can use any of the standard tools you're used to"
    (apply mutate g dissoc (keys @g))
    (is (empty? (keys @g)))

    (mutate g assoc :x 1 :y 2 :z 3)
    (is (= 6 (reduce + (vals @g))))

    (is (= 1 (@g :x)))))

(deftest data-structures
  (testing "interacting with maps feels the same"
    (mutate g assoc :test {:a 1})
    (let [m (agent (:test @g))]
      (mutate m assoc :b "x")
      (is (= "x" (get-in @g [:test :b])))

      (mutate m assoc :c 1)
      (is (= 1 (get @m :c)))

      (apply mutate m dissoc (keys @m))
      (mutate m assoc :x 2 :y 3 :z 4)
      (is (= 9 (reduce + (vals @m))))))
  (testing "sets behave as expected"
    (mutate g assoc :test #{1 2 3})
    (let [s (agent (@g :test))]
      (mutate g 
  (quote "vectors")
  (quote "nested data structures"))

(deftest concurrency-behavior
  (quote "reference original in middle of sent function"))

(deftest reserved-internals
  (quote ":- namespace"))
