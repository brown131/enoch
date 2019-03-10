(ns enoch.core
  (:require [clojure.core.async :as async]
            [enoch.center :refer [center-servos]]
            [enoch.config :refer [config-properties]]
            [enoch.driver :refer :all]
            [enoch.microphone :refer [go-microphone]]
            [enoch.motor-shield :refer [gpio-shutdown ultrasonic-stop]]
            [enoch.sensor :refer [do-ultrasonic-sensor]]
            [enoch.speaker :refer [go-speaker]])
  (:gen-class))

(defn shutdown [audio-chan drive-chan shutdown-chan]
  (ultrasonic-stop 1)
  (async/close! audio-chan)
  (async/close! drive-chan)
  (async/close! shutdown-chan)
  (gpio-shutdown))

(defn -main [& args]
  (if (= (first args) "--center")
    (center-servos)
    (let [audio-chan (async/chan 100)
          drive-chan (async/chan 10)
          shutdown-chan (async/chan 10)]
      (try
        (do-driver drive-chan shutdown-chan)
        (do-ultrasonic-sensor (:ultrasonic-sensor-boundary @config-properties) drive-chan)
        (go-microphone audio-chan)
        (go-speaker audio-chan)

        (async/put! drive-chan [:forward 20])
        (async/<!! shutdown-chan)
        (finally
          (shutdown audio-chan drive-chan shutdown-chan))))))
