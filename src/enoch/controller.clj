(ns enoch.controller
  (:require [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [enoch.config :refer :all]
            [enoch.driver :refer :all]))

(log/refer-timbre)

(def command-actions {:forward  drive-forward
                      :reverse  drive-reverse
                      :left     drive-left
                      :right    drive-right
                      :faster   drive-faster
                      :slower   drive-slower
                      :stop     drive-stop                      
                      :shutdown #(async/close! %)})

(defn go-process-command [command-chan speaker-chan shutdown-chan]
  (async/go-loop [command (async/<! command-chan)]
    (when command
      (log/debug "Command:" command)
      (let [speed (:default-speed @config-properties)] ; TODO
        (case command
          (:forward :reverse :left :right :faster :slower)
          (do
            (async/put! speaker-chan (str "Going " (symbol command)))
            ((command command-actions)))
          :stop (do
                  (async/put! speaker-chan "Stopping" )
                  ((command command-actions)))
          :shutdown (do
                      (async/put! speaker-chan "Shutting down" )
                      ((command command-actions) shutdown-chan)))
        (recur (async/<! command-chan))))))
