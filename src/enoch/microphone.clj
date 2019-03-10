(ns enoch.microphone
  (:require [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [enoch.config :refer [config-properties secret-properties]])
  (:import [javax.sound.sampled AudioSystem AudioFormat DataLine DataLine$Info TargetDataLine
                                AudioInputStream AudioFileFormat AudioFileFormat$Type]
           [java.nio ByteBuffer ShortBuffer ByteOrder]))

(log/refer-timbre)

(def audio-format (AudioFormat. 16000 16 1 true true))

(defn peak-to-peak-average [shorts]
  (/ (double (reduce (fn [a s] (+ a (max s (- s)))) shorts)) (count shorts)))

(defn filter-sound "Check if there was any sound in the buffer."
  [bytes num-bytes]
  (let [byte-buffer (ByteBuffer/wrap bytes)
        shorts (for [i (range 0 num-bytes 2)] (.getShort byte-buffer i))
        avgpp (peak-to-peak-average shorts)]
    (log/debug avgpp)
    (> avgpp (:noise-threshold @config-properties))))

(defn go-microphone "Read audio from the microphone an put in onto the audio channel."
  [audio-chan]
  (let [data-line-info (DataLine$Info. TargetDataLine audio-format)
        target-data-line (AudioSystem/getLine data-line-info)
        buffer-len (/ (.getBufferSize target-data-line) 5)
        buffer (byte-array buffer-len)]
    (async/go
      (try
        (.open target-data-line audio-format)
        (.start target-data-line)
        (loop [num-bytes-read (.read target-data-line buffer 0 buffer-len)
               empty-frames (:max-empty-frames @config-properties)]
          (when (pos? num-bytes-read)
            (let [has-sound? (filter-sound buffer buffer-len)]
              (when (or has-sound? (< empty-frames (:max-empty-frames @config-properties)))
                (log/debug "put!" has-sound? empty-frames)
                (async/put! audio-chan buffer))
              (recur (.read target-data-line buffer 0 buffer-len)
                     (if has-sound? 0 (inc empty-frames))))))
        (catch Exception e
          (log/error e "Error reading microphone."))))))
