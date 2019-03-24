(ns enoch.driver "Literally drives the car."
  (:require [taoensso.timbre :as log]
            [enoch.motor-shield :refer :all]))

(log/refer-timbre)

(def car-state (atom {:mode :stop :speed 0}))

(def direction-arrows {:forward 3 :right 4 :reverse 1 :left 2})

(defn change-arrow [direction]
  (let [arrow (direction direction-arrows)]
    (when-let [prev-arrow ((:mode @car-state) direction-arrows)]
      (when-not (= prev-arrow arrow)
        (arrow-off prev-arrow)))
    (when-not (= direction :stop)
      (arrow-on arrow))))

(defn drive-stop
  ([_] (drive-stop))
  ([]
    (when-not (= (:mode @car-state) :stop)
      (change-arrow :stop)
      (doseq [i [1 2 3 4]]
        (motor-stop i))
      (Thread/sleep 250)
      (swap! car-state assoc :mode :stop :speed 0))))

(defn change-state [direction speed]
  (when (direction direction-arrows)
    (change-arrow direction)

    ;; Stop car first if moving in a new direction.
    (when-not (= (:mode @car-state) direction)
      (drive-stop))
  
    (swap! car-state assoc :mode direction :speed speed)))
  
(defn drive-forward [speed]
  (change-state :forward speed)
  (doseq [i [1 2 3 4]]
    (motor-forward i speed)))
     
(defn drive-reverse [speed]
  (change-state :reverse speed)
  (doseq [i [1 2 3 4]]
    (motor-reverse i speed)))
     
(defn drive-left [speed]
  (change-state :left speed)
  (doseq [i [3 4]]
     (motor-forward i speed))
  (doseq [i [1 2]]
     (motor-forward i (/ speed 2))))
     
(defn drive-right [speed]
  (change-state :right speed)
  (doseq [i [1 2]]
    (motor-forward i speed))
  (doseq [i [3 4]]
    (motor-forward i (/ speed 2))))
