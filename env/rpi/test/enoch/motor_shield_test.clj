(ns enoch.motor-shield-test
  (:require [clojure.test :refer :all]
            [enoch.motor-shield :refer :all]))

(deftest test-arrows
  (doseq [i (range 1 5)]
    (arrow-on i)
    (Thread/sleep 500)
    (arrow-off i))
  (gpio-shutdown))

(deftest test-motor-1
  (doseq [i (range 1 5)]
    (motor-forward i 40)
    (Thread/sleep 1000)
    (motor-forward i 80)
    (Thread/sleep 1000)
    (motor-reverse i 20)
    (Thread/sleep 1000)
    (motor-stop i))
   (gpio-shutdown))

(deftest test-servo-horizontal
  (let [wait 25
        step 1]
    (println "rotate left")
    (servo-rotate 1 wait #(range 180 70 (* -1 step)))
   (println "rotate right")
    (servo-rotate 1 wait #(range 70 130 step)))
  (servo-stop 1)
  (gpio-shutdown))

(deftest test-servo-vertical
  (let [wait 25
        step 1]
    (println "rotate back")
    (servo-rotate 2 wait #(range 180 70 (* -1 step)))
    (println "rotate forward")
    (servo-rotate 2 wait #(range 70 130 step)))
  (servo-stop 2)
  (gpio-shutdown))

(deftest test-ultrasonic
  (let [boundary 10.0
        now (System/currentTimeMillis)]
     (loop [distance (ultrasonic-check 1)]
       (println "distance" distance)
       (when (and distance (< distance boundary))
         (println "boundary breached!"))
       (when (< (- (System/currentTimeMillis) now) 5000)
         (recur (ultrasonic-check 1)))))
  (ultrasonic-stop 1)
  (gpio-shutdown))
       