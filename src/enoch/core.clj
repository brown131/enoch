(ns enoch.core
  (:require [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [enoch.center :refer [center-servos]]
            [enoch.config :refer :all]
            [enoch.controller :refer :all]
            [enoch.driver :refer :all]
            [enoch.microphone :refer [go-microphone]]
            [enoch.motor-shield :refer [gpio-shutdown ultrasonic-stop]]
            [enoch.sensor :refer [do-ultrasonic-sensor]]
            [enoch.speaker :refer [go-speaker]]
            [enoch.speech-client :refer :all])
  (:gen-class))

(log/refer-timbre)

(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread e]
      (log/error e "Uncaught exception on" (.getName thread)))))

(defn shutdown "Close channels, stop tasks, and free resources."
  [microphone-chan response-chan action-chan shutdown-chan]
  (log/info "Shutting down.")
  (try
    (async/close! microphone-chan)
    (async/close! response-chan)
    (async/close! action-chan)
    (async/close! shutdown-chan)
    (ultrasonic-stop 1)
    (gpio-shutdown)
    (catch Exception e
      (log/error e "Error shutting down"))))

(defn -main [& args]
  (log/set-config! @logger-config)
  (if (= (first args) "--center")
    (center-servos)
    (let [microphone-chan (async/chan 50)
          response-chan (async/chan 50)
          action-chan (async/chan 50)
          shutdown-chan (async/chan 10)]
      (try
        (log/info "Staring enoch")

        ;; Start go-blocks.
        (go-microphone microphone-chan)
        (go-send-stt-request microphone-chan response-chan shutdown-chan)
        (go-process-stt-response response-chan action-chan)
        (go-process-action action-chan shutdown-chan)
        ;(go-speaker microphone-chan)

        ;; Start I/O threads.
        (do-ultrasonic-sensor (:ultrasonic-sensor-boundary @config-properties) action-chan)

        (async/<!! shutdown-chan)
        (catch Exception e
          (log/error e "Error running enoch"))
        (finally
          (shutdown microphone-chan response-chan action-chan shutdown-chan))))))
