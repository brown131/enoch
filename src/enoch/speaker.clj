(ns enoch.speaker
  (:require [clojure.core.async :as async]
            [clojure.java.io :refer [file output-stream]])
  (:import [javax.sound.sampled AudioSystem AudioFormat DataLine DataLine$Info SourceDataLine
                                AudioInputStream AudioFileFormat AudioFileFormat$Type]))

(def audio-format (AudioFormat. 16000 16 1 true true))

(defn go-speaker "Read data from the audio channel and send it to the speaker."
  [audio-chan]
  (let [data-line-info (DataLine$Info. SourceDataLine audio-format)
        source-data-line (AudioSystem/getLine data-line-info)]
    (async/go
      (try
        (loop [data (async/<! audio-chan)]
          (when data
            (with-open [out (output-stream (file "eno.wav"))]
              (.write out (byte-array 1000)))
            (.write source-data-line data 0 (count data))
            (recur (async/<! audio-chan))))
        (catch Exception e
          (.printStackTrace e))))))