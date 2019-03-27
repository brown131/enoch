(ns enoch.sensor
  (:require  [clojure.core.async :as async]
             [taoensso.timbre :as log]
             [enoch.motor-shield :refer :all]))

(log/refer-timbre)

(defn do-ultrasonic-sensor [boundary command-chan]
  (async/thread
    (loop [distance (ultrasonic-check :front)]
      (when distance
        (when (and (not= distance :timed-out) (< distance boundary))
          (log/info "Ultrasonic boundary breached")
          (async/put! command-chan :stop))
        (recur (ultrasonic-check :front))))))
