(ns enoch.driver "Literally drives the car."
  (:require [enoch.motor-shield :as ms]))

(def car-state (atom {:mode :stopped :speed 0}))

(def arrows {:forward 3 :right 4 :reverse 1 :left 2})

(defn change-arrow [direction]
  (let [arrow (direction arrows)]
    (when-let [prev-arrow ((:mode @car-state) arrows)]
      (when-not (= prev-arrow arrow)
        (ms/arrow-off prev-arrow)))
    (when-not (= direction :stopped)
      (ms/arrow-on arrow))))

(defn drive-stop []
  (when-not (= (:mode @car-state) :stopped)
    (change-arrow :stopped)
    (doseq [i [1 2 3 4]]
      (ms/motor-stop i))
    (Thread/sleep 250)
    (swap! car-state assoc :mode :stopped :speed 0)))

(defn change-state [direction speed]
  (when-let [arrow (direction arrows)]
    (change-arrow direction)

    ;; Stop car first if moving in a new direction.
    (when-not (= (:mode @car-state) direction)
      (drive-stop))
  
    (swap! car-state assoc :mode direction :speed speed)))
  
(defn drive-forward [speed]
  (change-state :forward speed)
  (doseq [i [1 2 3 4]]
    (ms/motor-forward i speed)))
     
(defn drive-reverse [speed]
  (change-state :reverse speed)
  (doseq [i [1 2 3 4]]
    (ms/motor-reverse i speed)))
     
(defn drive-left [speed]
  (change-state :left speed)
  (doseq [i [3 4]]
     (ms/motor-forward i speed))
  (doseq [i [1 2]]
     (ms/motor-reverse i speed)))
     
(defn drive-right [speed]
  (change-state :right speed)
  (doseq [i [1 2]]
    (ms/motor-forward i speed))
  (doseq [i [3 4]]
    (ms/motor-reverse i speed)))