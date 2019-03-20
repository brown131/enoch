(ns enoch.controller
  (:require [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [enoch.config :refer :all]
            [enoch.driver :refer :all]))

(log/refer-timbre)

(defn go-process-action [action-chan shutdown-chan]
  (async/go-loop [action (async/<! action-chan)]
    (when action
      (log/debug "Action:" action)
      (let [speed (:default-speed @config-properties)] ; TODO
        (case action
          :forward  (drive-forward speed)
          :reverse  (drive-reverse speed)
          :left     (drive-left speed)
          :right    (drive-right speed)
          :stop     (drive-stop)
          :shutdown (async/close! shutdown-chan))
        (recur (async/<! action-chan))))))
