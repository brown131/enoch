(ns enoch.motor-shield-test
  (:require [clojure.test :refer :all]
            [enoch.motor-shield :refer :all]))

(deftest test-arrows
  (doseq [id [:front :back :left :right] ]
    (arrow-on id)
    (Thread/sleep 500)
    (arrow-off i)d)
  (gpio-shutdown))

(deftest test-motor-1
  (doseq [id [:front-left :front-right :back-left :back-right]]
    (motor-forward id 40)
    (Thread/sleep 1000)
    (motor-forward id 80)
    (Thread/sleep 1000)
    (motor-reverse id 20)
    (Thread/sleep 1000)
    (motor-stop id))
   (gpio-shutdown))

(deftest test-servo-horizontal
  (let [wait 25
        step 1]
    (println "rotate left")
    (servo-rotate :horizontal wait #(range 180 70 (* -1 step)))
   (println "rotate right")
    (servo-rotate :horizontal wait #(range 70 130 step)))
  (servo-stop :horizontal)
  (gpio-shutdown))

(deftest test-servo-vertical
  (let [wait 25
        step 1]
    (println "rotate back")
    (servo-rotate :vertical wait #(range 180 70 (* -1 step)))
    (println "rotate forward")
    (servo-rotate :vertical wait #(range 70 130 step)))
  (servo-stop :vertical)
  (gpio-shutdown))

(deftest test-ultrasonic
  (let [boundary 10.0
        now (System/currentTimeMillis)]
     (loop [distance (ultrasonic-check :front)]
       (println "distance" distance)
       (when (and (not= distance :timed-out)
                  (< distance boundary))
         (println "boundary breached!"))
       (when (< (- (System/currentTimeMillis) now) 5000)
         (recur (ultrasonic-check :front)))))
  (ultrasonic-stop :front)
  (gpio-shutdown))
       