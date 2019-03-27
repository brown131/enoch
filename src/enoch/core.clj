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
  [microphone-chan speaker-chan response-chan command-chan shutdown-chan]
  (try
    (async/close! microphone-chan)
    (async/close! speaker-chan)
    (async/close! response-chan)
    (async/close! command-chan)
    (async/close! shutdown-chan)
    (ultrasonic-stop :front)
    (gpio-shutdown)
    (catch Exception e
      (log/error e "Error shutting down"))
    (finally
      (log/info "Shut down."))))

(defn -main [& args]
  (log/set-config! @logger-config)
  (when (= (first args) "--center")
    (center-servos))
  (let [microphone-chan (async/chan 50)
        speaker-chan (async/chan 50)
        response-chan (async/chan 50)
        command-chan (async/chan 50)
        shutdown-chan (async/chan 10)]
    (try
      (log/info "Starting enoch")

      ;; Start go-blocks.
      (go-microphone microphone-chan)
      (go-speaker speaker-chan)
      (go-send-stt-request microphone-chan response-chan shutdown-chan)
      (go-process-stt-response response-chan command-chan speaker-chan)
      (go-process-command command-chan speaker-chan shutdown-chan)

      ;; Start I/O threads.
      (do-ultrasonic-sensor (:ultrasonic-sensor-boundary @config-properties) command-chan)

      (async/<!! shutdown-chan)
      (catch Exception e
        (log/error e "Error running enoch"))
      (finally
        (shutdown microphone-chan speaker-chan response-chan command-chan shutdown-chan)))))
