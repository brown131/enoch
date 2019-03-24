(ns enoch.speaker
  (:require [clojure.core.async :as async]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as log]
            [clojure.java.io :refer [file output-stream]]))

(log/refer-timbre)

(def speaking? (atom nil))

(def espeak-args {:amplitude    "-a"
                  :word-gap     "-g"
                  :capitals     "-k"
                  :line-length  "-l"
                  :pitch        "-p"
                  :speed        "-s"
                  :voice        "-v"})

(defn espeak [text & args]
  (let [cmd (reduce #(if ((first %2) espeak-args)
                       (conj %1 ((first %2) espeak-args) (str (second %2)))
                       (log/error "Invalid argument" (first %2)))
                    ["espeak" (format "'%s'" text)] (first args))]
    (reset! speaking? true)
    (log/debug "Saying:" text)
    (apply sh cmd)
    (reset! speaking? false)))

(defn go-speaker "Read data from the audio channel and send it to the speaker."
  [speaker-chan]
  ;; TODO
  (async/go
    (try
      (loop [text (async/<! speaker-chan)]
        (when text
          (espeak text {:amplitude 50 :pitch 90 :speed 150 :voice "en-gb"})
          (recur (async/<! speaker-chan))))
      (catch Exception e
        (log/error e "Error playing speaker.")))))
