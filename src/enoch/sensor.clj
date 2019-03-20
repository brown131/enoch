(ns enoch.sensor
  (:require  [clojure.core.async :as async]
             [taoensso.timbre :as log]
             [enoch.motor-shield :refer :all]))

(log/refer-timbre)

(defn do-ultrasonic-sensor [boundary action-chan]
  (async/thread
    (loop [distance (ultrasonic-check 1)]
      (when distance
        (when (and (not= distance :timed-out) (< distance boundary))
          ;; TODO Have it back-up from the obstacle and continue (:wander mode?)
	  (log/debug "Boundary breached")
          (async/put! action-chan :stop))
        (recur (ultrasonic-check 1))))))
