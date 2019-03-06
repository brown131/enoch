(ns enoch.core
  (:require [clojure.core.async :as async]
            [enoch.center :as c]
            [enoch.driver :refer :all]
            [enoch.motor-shield :refer :all])
  (:gen-class))

(def drive-chan (async/chan 10))
(def shutdown-chan (async/chan 10))

(def drive-directions {:forward drive-forward
		       :reverse drive-reverse
                       :left    drive-left
		       :right   drive-right
		       :stop    drive-stop})

(defn go-driver [drive-chan]
 (async/go
   (loop [[direction speed] (async/<! drive-chan)]
     (when direction
       ((get drive-directions direction) speed)
       (if (= direction :stop)
         ;; TODO Remove when we have a :shutdown voice command.
         (async/put! shutdown-chan true)
	 (recur (async/<! drive-chan)))))))

(defn go-ultrasonic-sensor [boundary]
  (async/go
    (loop [distance (ultrasonic-check 1)]
      (when distance
        (println "distance" distance)
        (if (and (not= distance :timed-out)
            (< distance boundary))
	  ;; TODO Have it back-up from the obstacle and continue (:wander mode?)
          (async/put! drive-chan [:stop 0])
	  (recur (ultrasonic-check 1)))))))

(defn shutdown []
  (ultrasonic-stop 1)
  (async/close! drive-chan)
  (async/close! shutdown-chan)
  (gpio-shutdown))

(defn -main [& args]
  (if (= (first args) "--center")
    (c/center-servos)
    (try
      (go-driver drive-chan)
      (go-ultrasonic-sensor 20.0)
      (async/put! drive-chan [:forward 20])
      (async/<!! shutdown-chan)
      (finally
        (shutdown)))))
      
