(ns enoch.microphone
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [enoch.config :refer [config-properties secret-properties]])
  (:import [javax.sound.sampled AudioSystem AudioFormat DataLine$Info TargetDataLine]
           [java.io ByteArrayOutputStream]
           [java.lang Math Short System]
           [java.nio ByteBuffer ShortBuffer]))

(log/refer-timbre)

(def audio-format (AudioFormat. 16000 16 1 true true))

(defn long->little-endian-bytes [num]
  (mapv #(unchecked-byte (bit-and 0xff (bit-shift-right num (* 8 %)))) (range 0 4)))

(defn short->little-endian-bytes [num]
  (mapv #(bit-and 0xff (bit-shift-right num (* 8 %))) (range 0 2)))

(defn shorts->little-endian-bytes "Converts an array of shorts to a byte array in litle-endian order."
  [shorts]
  (byte-array (reduce #(conj %1 (unchecked-byte (bit-and %2 0xff))
                             (unchecked-byte (bit-shift-right %2 8))) [] shorts)))

(def wave-header (reduce into (mapv byte "WAVE")             ; format
                         [(mapv byte "fmt ")                 ; subchunk 1 id
                          (long->little-endian-bytes 16)     ; subchunk size
                          (short->little-endian-bytes 1)     ; audio format PCM=1 (little-endian)
                          (short->little-endian-bytes 1)     ; num channels mono=1 (little-endian)
                          (long->little-endian-bytes 16000)  ; sample rate
                          (long->little-endian-bytes 32000)  ; byte rate
                          (short->little-endian-bytes 2)     ; block align
                          (short->little-endian-bytes 16)    ; bits per sample
                          (mapv byte "data")]))              ; subchunk 2 id

(defn bytes->shorts "Converts a byte array to a short array."
  [bytes num-bytes]
  (let [short-buffer (ShortBuffer/allocate (/ num-bytes 2))]
    (.array (.put short-buffer (.asShortBuffer (ByteBuffer/wrap bytes))))))

(defn shorts->bytes "Converts an array of shorts to a byte array."
  [shorts]
  (byte-array (reduce #(conj %1 (byte (bit-shift-right %2 8)) (byte (bit-and %2 0xff))) [] shorts)))

(defn root-mean-square "Calculate the root mean square of the buffer."
  [shorts num-shorts]
  (Math/sqrt (/ (reduce #(+ %1 (* %2 %2)) 0.0 shorts) num-shorts)))

(defn peak-value "Find the maximum wave height in a buffer."
  [shorts]
  (reduce #(max %1 (Math/abs (int %2))) 0 shorts))

(defn amplify-sound-clip
  [shorts peak]
  (map #(cond
          (pos? %) (short (min (* % (/ 32767.0 peak)) 32767.0))
          (neg? %) (short (* % (/ 32767.0 peak)))
          :else %) shorts))

(defn create-wave-buffer [wave-data num-bytes]
  (reduce into (mapv byte "RIFF")                             ; chunk id
               [(long->little-endian-bytes (+ 36 num-bytes))  ; chunk size
                wave-header
                (long->little-endian-bytes num-bytes)
                ;; These are little-endian shorts. RIFX allows for big-endian but does the API support it?
                (shorts->little-endian-bytes wave-data)]))    ; subchunk 2 size

(defn end-the-clip [output-buffer-stream microphone-chan]
  ;; TODO Reduce number of passes through the data
  (let [num-bytes (.size output-buffer-stream)
        bytes (.toByteArray output-buffer-stream)
        shorts (bytes->shorts bytes num-bytes)
        peak (peak-value shorts)
        clip (amplify-sound-clip shorts peak)
        wave-buffer (create-wave-buffer clip num-bytes)]
    ;; Wave file is for testing only.
    (with-open [out (io/output-stream (io/file (str "/tmp/" (System/currentTimeMillis) ".wav")))]
      (.write out (byte-array wave-buffer) 0 (count wave-buffer)))

    (async/put! microphone-chan wave-buffer)))

(defn go-microphone "Read audio from the microphone an put in onto the audio channel."
  [microphone-chan]
  (let [target-data-line-info (DataLine$Info. TargetDataLine audio-format)
        target-data-line (AudioSystem/getLine target-data-line-info)
        num-bytes (.getBufferSize target-data-line)
        bytes (byte-array num-bytes)]
    (async/go
      (try
        (.open target-data-line)
        (.start target-data-line)
        (loop [num-bytes-read (.read target-data-line bytes 0 num-bytes)
               output-buffer-stream (ByteArrayOutputStream.)
               empty-frames (:max-empty-frames @config-properties)]
          (when (pos? num-bytes-read)
            (let [shorts (bytes->shorts bytes num-bytes-read)
                  rms (root-mean-square shorts (/ num-bytes-read 2))
                  has-sound? (> rms (:noise-threshold @config-properties))
                  end-of-clip? (and (pos? (.size output-buffer-stream))
                                    (>= empty-frames (:max-empty-frames @config-properties)))]
              ;(log/debug "hs?" has-sound? "eoc?" end-of-clip? "ef" empty-frames)
              (if end-of-clip?
                (end-the-clip output-buffer-stream microphone-chan)
                (when (or has-sound? (zero? empty-frames)) ; Include 1 empty frame at end to avoid clipping.
                  (when (pos? empty-frames)
                    (log/debug "Sound detected."))
                  (.write output-buffer-stream bytes 0 num-bytes)))
                (recur (.read target-data-line bytes 0 num-bytes)
                       (if end-of-clip? (ByteArrayOutputStream.) output-buffer-stream)
                       (if has-sound? 0 (inc empty-frames))))))
        (catch Exception e
          (.printStackTrace e)
          (log/error e "Error reading microphone."))
        (finally
          (.stop target-data-line)
          (.close target-data-line))))))
