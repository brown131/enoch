(ns enoch.controller
  (:require [clojure.core.async :as async]
            [enoch.driver :refer :all]))

(defn go-process-action [action-chan shutdown-chan]
  (async/go-loop [action (async/<! action-chan)]
    (when action
      (let [speed 20] ; TODO
        (case action
          :forward  (drive-forward speed)
          :reverse  (drive-reverse speed)
          :left     (drive-left speed)
          :right    (drive-right speed)
          :stop     (drive-stop)
          :shutdown (async/close! shutdown-chan))
        (recur (async/<! action-chan))))))
