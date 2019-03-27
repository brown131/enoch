(ns enoch.center
  "Centers the horizontal and vertical servos so they face forward."
  (:require [enoch.motor-shield :refer :all]
            [taoensso.timbre :as log]))

(log/refer-timbre)

(def step 1)
(def wait 30)

(defn center-servos []
  (try
    (info "Centering horizontal and vertical servos.")

    (servo-rotate :horizontal wait #(range 180 70 (* -1 step)))
    (servo-rotate :horizontal wait #(range 70 130 step))
    (servo-stop :horizontal)

    (servo-rotate :vertical wait #(range 180 70 (* -1 step)))
    (servo-rotate :vertical wait #(range 70 130 step))
    (servo-stop :vertical)

    (gpio-shutdown)
    (catch Exception e
      (log/error e "Error centering servos"))))
