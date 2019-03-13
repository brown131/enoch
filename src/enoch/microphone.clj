(ns enoch.microphone
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [enoch.config :refer [config-properties secret-properties]])
  (:import [javax.sound.sampled AudioSystem AudioFormat DataLine$Info TargetDataLine
                                AudioInputStream AudioFileFormat AudioFileFormat$Type]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.nio ByteBuffer]))

(log/refer-timbre)

(def audio-format (AudioFormat. 16000 16 1 true true))

(defn peak-to-peak-average [shorts]
  (/ (double (reduce (fn [a s] (+ a (max s (- s)))) shorts)) (count shorts)))

(defn filter-sound "Check if there was any sound in the buffer."
  [bytes num-bytes]
  (let [byte-buffer (ByteBuffer/wrap bytes)
        shorts (for [i (range 0 num-bytes 2)] (.getShort byte-buffer i))
        avgpp (peak-to-peak-average shorts)]
    ;(log/debug avgpp)
    (> avgpp (:noise-threshold @config-properties))))

(defn go-microphone "Read audio from the microphone an put in onto the audio channel."
  [audio-chan]
  (let [target-data-line-info (DataLine$Info. TargetDataLine audio-format)
        target-data-line (AudioSystem/getLine target-data-line-info)
        buffer-len (.getBufferSize target-data-line)
        buffer (byte-array buffer-len)]
    (async/go
      (try
        (.open target-data-line)
        (.start target-data-line)
        (loop [num-bytes-read (.read target-data-line buffer 0 buffer-len)
               output-buffer-stream (ByteArrayOutputStream.)
               empty-frames (:max-empty-frames @config-properties)]
          (when (pos? num-bytes-read)
            (let [has-sound? (filter-sound buffer buffer-len)
                  end-of-clip? (and (pos? (.size output-buffer-stream))
                                    (>= empty-frames (:max-empty-frames @config-properties)))]
              (log/debug "hs?" has-sound? "eoc?" end-of-clip? "ef" empty-frames)
              (if end-of-clip?
                (let [input-buffer-stream (ByteArrayInputStream. (.toByteArray output-buffer-stream))
                      wave-audio-stream (AudioInputStream. input-buffer-stream audio-format
                                                           (/ (.size output-buffer-stream)
                                                              (.getFrameSize audio-format)))
                      wave-file-name (str "/tmp/" (System/currentTimeMillis) ".wav")]
                  ;; Create a wave file from the buffers sounds.
                  (AudioSystem/write wave-audio-stream AudioFileFormat$Type/WAVE (io/file wave-file-name))
                  (.flush output-buffer-stream)
                  (.close output-buffer-stream)
                  (.close wave-audio-stream)
                  (async/put! audio-chan wave-file-name))
                (when has-sound?
                  (.write output-buffer-stream buffer 0 buffer-len)))
                (recur (.read target-data-line buffer 0 buffer-len)
                       (if end-of-clip? (ByteArrayOutputStream.) output-buffer-stream)
                       (if has-sound? 0 (inc empty-frames))))))
        (catch Exception e
          (.printStackTrace e)
          (log/error e "Error reading microphone."))
        (finally
          (.stop target-data-line)
          (.close target-data-line))))))
