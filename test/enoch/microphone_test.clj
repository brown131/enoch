(ns enoch.microphone-test
  (:require [clojure.test :refer :all]
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

(deftest test-amplify-sound-clip
  (let [clip (short-array [1 11 20 110 200 210 110 110 120 31 2 10])
        rms (root-mean-square clip (count clip))]
    (println rms)
    (is (= '(0 0 0 220 400 420 220 220 240 0 0 0)
           (amplify-sound-clip clip 2.0 rms)))))

(deftest test-create-wave-buffer
  (let [obs (ByteArrayOutputStream.)
        bytes (byte-array [2 1 0 11 0 20 0 110 0 200 0 210 0 110 0 110 0 120 0 31 0 2 1 10])]
    (.write obs bytes 0 (count bytes))
    (mapv #(print % " ") bytes)
    (is (= (gen-wave-file (bytes->shorts bytes (count bytes)))
           (mapv identity (create-wave-buffer obs))))))