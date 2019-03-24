(ns enoch.microphone-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [chan <!!]]
            [enoch.microphone :refer :all])
  (:import [java.io ByteArrayOutputStream]))

(deftest test-bytes->shorts
  (is (= (mapv identity (short-array [512 257 258]))
         (mapv identity (bytes->shorts (byte-array [2 0 1 1 1 2]) 6)))))

(deftest test-shorts->bytes
  (is (= [0 1 2 3]
         (mapv identity (shorts->bytes (short-array [1 515]))))))

(deftest test-root-mean-square
  (let [rms (root-mean-square [2 3 4] 3)]
    (is (and (> rms 3.10) (< rms 3.11)))))

(deftest test-peak-value
  (is (= 7 (peak-value (short-array [1 2 7 4 3])))))

(deftest test-amplify-sound-clip
  (let [clip (short-array [1 11 20 110 200 210 110 110 120 31 2 10])
        rms (root-mean-square clip (count clip))
        peak (peak-value clip)]
    (println rms)
    (is (= '(1 11 20 17163 31206 32767 17163 17163 18724 31 2 10)
           (amplify-sound-clip clip peak)))))

(deftest test-create-wave-buffer
  (let [clip (short-array [258 11 20 110 200 210 110 120 31 513 10])
        little-endian-bytes (byte-array [2 1 11 0 20 0 110 0 200 0 210 0 110 0 120 0 31 0 1 2 10 0])
        num-bytes (* 2 (count clip))
        wave-buffer (create-wave-buffer clip num-bytes)]
    (is (= 44 (- (count wave-buffer) num-bytes)))
    (is (= (mapv identity little-endian-bytes)
           (take-last num-bytes (mapv identity wave-buffer))))))

(deftest test-end-the-clip
  (let [bytes (byte-array [2 1 0 11 0 20 0 110 0 200 0 210 0 110 0 110 0 120 0 31 0 2 1 10])
        obs (ByteArrayOutputStream.)
        ch (chan 10)]
    (.write obs bytes 0 (count bytes))
    (end-the-clip obs ch)
    (let [clip (<!! ch)]
      (is (= [82 73 70 70 60 0 0 0 87 65 86 69 ; RIFF
              102 109 116 32 16 0 0 0 1 0 1 0 -128 62 0 0 0 125 0 0 2 0 16 0 ; fmt
              100 97 116 97 24 0 0 0 ; data
              -1 127 11 0 20 0 110 0 -26 49 101 52 110 0 110 0 120 0 31 0 2 0 94 66]
             (mapv identity clip))))))
