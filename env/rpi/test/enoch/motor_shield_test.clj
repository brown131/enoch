(ns enoch.motor-shield-test
  (:require [clojure.test :refer :all]
            [enoch.motor-shield :refer :all]))

#_(deftest test-arrows
  (doseq [i (range 1 5)]
    (arrow-on i)
    (Thread/sleep 500)
    (arrow-off i))
  (gpio-shutdown))

#_(deftest test-motor-1
  (doseq [i (range 1 5)]
    (motor-forward i 40)
    (Thread/sleep 1000)
    (motor-forward i 80)
    (Thread/sleep 1000)
    (motor-reverse i 20)
    (Thread/sleep 1000)
    (motor-stop i))
   (gpio-shutdown))

(deftest test-servo
  (servo-rotate 1 100 #(range 0 (inc 10) 1))
  (servo-rotate 1 100 #(range (inc 10) 0 -1)))
  
