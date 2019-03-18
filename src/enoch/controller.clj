(ns enoch.controller
  (:require [clojure.core.async :as async]
            [enoch.driver :refer :all]))

(defn go-process-action [action-chan drive-chan shutdown-chan]
  (async/go-loop [action (async/<! action-chan)]
    (when action
      (case action
        (keys drive-commands) (async/put! drive-chan (action drive-commands))
        :shutdown (async/close! shutdown-chan))
      (recur (async/<! action-chan)))))
