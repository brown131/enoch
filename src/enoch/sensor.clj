(ns enoch.sensor
  (:require  [clojure.core.async :as async]
             [enoch.motor-shield :refer :all]))

(defn do-ultrasonic-sensor [boundary drive-chan]
  (async/thread
    (loop [distance (ultrasonic-check 1)]
      (when distance
        (println "distance" distance)
        (if (and (not= distance :timed-out)
                 (< distance boundary))
          ;; TODO Have it back-up from the obstacle and continue (:wander mode?)
          (async/put! drive-chan [:stop 0])
          (recur (ultrasonic-check 1)))))))
