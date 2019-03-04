(ns enoch.core
  (:require [clojure.core.async :as async]
            [enoch.driver :refer :all]
            [enoch.motor-shield :refer :all])
  (:gen-class))

(def drive-directions {:forward drive-forward
		       :reverse drive-reverse
                       :left    drive-left
		       :right   drive-right
		       :stop    drive-stop})

(defn driver [drive-chan]
  (async/go
     (let [[direction speed] (async/<! drive-chan)]
       ((get drive-directions direction) speed)))) 

(defn ultrasonic-sensor [us-chan]
  (async/thread
    (loop [distance (ultrasonic-check]

(defn -main [& args]
  (try
    (let [boundary 10.0
          drive-chan (async/chan 10)]
      (driver driver-chan)
      (<!! drive-chan [:forward 20])
      (loop [distance (ultrasonic-check 1)]
        (println "distance" distance)
        (if (and (not= distance :timed-out)
	         (< distance boundary))
          (async/<!! drive-chan [:stop 0])
          (recur (ultrasonic-check 1)))))
    (finally
      (ultrasonic-stop 1)
      (gpio-shutdown))))
