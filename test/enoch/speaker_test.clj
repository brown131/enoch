(ns enoch.speaker-test
  (:require [clojure.test :refer :all]
            [enoch.speaker :refer :all]))

(deftest test-espeak
  (espeak "testing 1 2 3" {:amplitude 100 :pitch 100 :speed 100 :word-gap 10 :voice "en-sc"}))
