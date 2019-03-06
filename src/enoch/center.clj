(ns enoch.center
  "Centers the horizontal and vertical servos so they face forward."
  (:require [enoch.motor-shield :refer :all]))

(def step 1)
(def wait 30)

(defn center-servos []
  (println "Centering horizontal and vertical servos.")
  
  ;; Horizontal
  (servo-rotate 1 wait #(range 180 70 (* -1 step)))
  (servo-rotate 1 wait #(range 70 130 step))
  (servo-stop 1) 

  ;; Vertical
  (servo-rotate 2 wait #(range 180 70 (* -1 step)))
  (servo-rotate 2 wait #(range 70 130 step))
  (servo-stop 2)

  (gpio-shutdown))
