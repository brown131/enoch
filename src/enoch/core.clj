(ns enoch.core
  (:require [clojure.core.async :as async]
            [clojure.java.io :refer [file output-stream]]
            [enoch.center :refer [center-servos]]
            [enoch.config :refer [config-properties secret-properties]]
            [enoch.driver :refer :all]
            [enoch.motor-shield :refer :all])
  (:import [javax.sound.sampled AudioSystem AudioFormat DataLine DataLine$Info TargetDataLine SourceDataLine
                                AudioInputStream AudioFileFormat AudioFileFormat$Type]
           [java.io PipedInputStream PipedOutputStream]
           [java.nio ByteBuffer ShortBuffer ByteOrder])
  (:gen-class))

(def audio-chan (async/chan 100))  ;; TODO How much back pressure?
(def drive-chan (async/chan 10))
(def shutdown-chan (async/chan 10))

(def audio-format (AudioFormat. 16000 16 1 true true))

(def drive-directions {:forward drive-forward
		       :reverse drive-reverse
                       :left    drive-left
		       :right   drive-right
		       :stop    drive-stop})

(defn do-driver [drive-chan]
 (async/thread
   (loop [[direction speed] (async/<!! drive-chan)]
     (when direction
       ((get drive-directions direction) speed)
       (if (= direction :stop)
         ;; TODO Remove when we have a :shutdown voice command.
         (async/put! shutdown-chan true)
         (recur (async/<!! drive-chan)))))))

(defn do-ultrasonic-sensor [boundary]
  (async/thread
    (loop [distance (ultrasonic-check 1)]
      (when distance
        (println "distance" distance)
        (if (and (not= distance :timed-out)
                 (< distance boundary))
          ;; TODO Have it back-up from the obstacle and continue (:wander mode?)
          (async/put! drive-chan [:stop 0])
          (recur (ultrasonic-check 1)))))))

(defn peak-to-peak-average [shorts]
  (/ (double (reduce (fn [a s] (+ a (max s (- s)))) shorts)) (count shorts)))

(defn filter-sound "Check if there was any sound in the buffer."
  [bytes num-bytes]
  (let [byte-buffer (ByteBuffer/wrap bytes)
        shorts (for [i (range 0 num-bytes 2)] (.getShort byte-buffer i))
        avgpp (peak-to-peak-average shorts)]
    (println avgpp)
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
                (println "put!" has-sound? empty-frames)
                (async/put! audio-chan buffer))
              (recur (.read target-data-line buffer 0 buffer-len)
                     (if has-sound? 0 (inc empty-frames))))))
        (catch Exception e
          (.printStackTrace e))))))

(defn go-speaker "Read data from the audio channel and send it to the speaker."
  [audio-channel]
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

(defn go-connect-speech-api [] )

(defn go-connect-speech-api-response [] )

(defn shutdown []
  (ultrasonic-stop 1)
  (async/close! audio-chan)
  (async/close! drive-chan)
  (async/close! shutdown-chan)
  (gpio-shutdown))

(defn -main [& args]
  (if (= (first args) "--center")
    (center-servos)
    (try
      (do-driver drive-chan)
      (do-ultrasonic-sensor (:ultrasonic-sensor-boundary @config-properties))
      (go-microphone audio-chan)
      (go-speaker audio-chan)

      (async/put! drive-chan [:forward 20])
      (async/<!! shutdown-chan)
      (finally
        (shutdown)))))
      
