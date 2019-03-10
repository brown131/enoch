(ns enoch.core
  (:require [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [enoch.center :refer [center-servos]]
            [enoch.config :refer :all]
            [enoch.driver :refer :all]
            [enoch.microphone :refer [go-microphone]]
            [enoch.motor-shield :refer [gpio-shutdown ultrasonic-stop]]
            [enoch.sensor :refer [do-ultrasonic-sensor]]
            [enoch.speaker :refer [go-speaker]])
  (:gen-class))

(log/refer-timbre)

(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread e]
      (log/error e "Uncaught exception on" (.getName thread)))))

(defn shutdown [audio-chan drive-chan shutdown-chan]
  (try
    (ultrasonic-stop 1)
    (async/close! audio-chan)
    (async/close! drive-chan)
    (async/close! shutdown-chan)
    (gpio-shutdown)
    (catch Exception e
      (log/error e "Error shutting down"))))

(defn -main [& args]
  (log/set-config! @logger-config)
  (if (= (first args) "--center")
    (center-servos)
    (let [audio-chan (async/chan 100)
          drive-chan (async/chan 10)
          shutdown-chan (async/chan 10)]
      (try
        (log/info "Staring enoch")
        (do-driver drive-chan shutdown-chan)
        (do-ultrasonic-sensor (:ultrasonic-sensor-boundary @config-properties) drive-chan)
        (go-microphone audio-chan)
        (go-speaker audio-chan)

        (async/put! drive-chan [:forward 20])
        (async/<!! shutdown-chan)
        (catch Exception e
          (log/error e "Error running enoch"))
        (finally
          (shutdown audio-chan drive-chan shutdown-chan))))))
