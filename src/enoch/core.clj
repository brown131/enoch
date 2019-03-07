(ns enoch.core
  (:require [clojure.core.async :as async]
            [enoch.center :refer [center-servos]]
            [enoch.config :refer [config-properties private-properties]]
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

(defn do-driver [drive-chan]
 (async/thread
   (loop [[direction speed] (async/<!! drive-chan)]
     (when direction
       ((get drive-directions direction) speed)
       (if (= direction :stop)
         ;; TODO Remove when we have a :shutdown voice command.
         (async/put! shutdown-chan true)
         (recur (async/<!! drive-chan)))))))

(defn do-ultrasonic-sensor [boundary]
  (async/thread
    (loop [distance (ultrasonic-check 1)]
      (when distance
        (println "distance" distance)
        (if (and (not= distance :timed-out)
                 (< distance boundary))
          ;; TODO Have it back-up from the obstacle and continue (:wander mode?)
          (async/put! drive-chan [:stop 0])
          (recur (ultrasonic-check 1)))))))

(defn go-microphone [mic-chan] )

(defn go-connect-speech-api [] )

(defn go-connect-speech-api-response [] )

(defn shutdown []
  (ultrasonic-stop 1)
  (async/close! drive-chan)
  (async/close! shutdown-chan)
  (gpio-shutdown))

(defn -main [& args]
  (if (= (first args) "--center")
    (center-servos)
    (try
      (do-driver drive-chan)
      (do-ultrasonic-sensor (:ultrasonic-sensor-boundary @config-properties))

      (async/put! drive-chan [:forward 20])
      (async/<!! shutdown-chan)
      (finally
        (shutdown)))))
      
