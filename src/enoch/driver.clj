(ns enoch.driver "Literally drives the car."
  (:require [taoensso.timbre :as log]
            [enoch.motor-shield :refer :all]))

(log/refer-timbre)

;;; Car directions:
;;;  left  forward  right
;;;           1
;;;      6    |    2
;;;        \  |  /
;;;          \|/
;;;     stop  0
;;;          /|\
;;;        /  |  \
;;;      5    |    3
;;;           4
;;;        reverse

;; Car direction transitions
;; (get-in car-transitions [1 :right]) = 2 (forward-right)
;; Negative means to stop before changing to the direction, e.g. -4 = stop then reverse
(def car-transitions [{:forward 1 :reverse 4 :right 2 :left 6}     ; stop
                      {:reverse -4 :right 2 :left 6 :stop 0}       ; forward
                      {:forward 1 :reverse -4 :left 6 :stop 0}     ; forward-right
                      {:forward -1 :reverse 4 :left 5 :stop 0}     ; reverse-right
                      {:forward -1 :right 3 :left 5 :stop 0}       ; reverse
                      {:forward -1 :reverse 4 :right 3 :stop 0}    ; reverse-left
                      {:forward 1 :reverse -4 :right 2 :stop 0}])  ; forward-left

(def car-state (atom {:direction 0 :speed 0}))

(def direction-arrows {:forward 3 :reverse 1 :right 4 :left 2})

(def arrow-transitions [; stop
                        {:forward {:forward true}
                         :reverse {:reverse true}
                         :right {:forward true :right true}
                         :left {:forward true :fright true}}
                        ;; forward
                        {:reverse {:forward false :reverse true}
                         :right {:right true}
                         :left {:left true}
                         :stop {:forward false}}
                        ;; forward-right
                        {:forward {:right false}
                         :reverse {:forward false :reverse true :right false}
                         :left {:right false :left true}
                         :stop {:forward false :right false}}
                        ;; reverse-right
                        {:forward {:forward true :reverse false :right false}
                         :reverse {:right false}
                         :left {:right false :left true}
                         :stop {:reverse false :right false}}
                        ;; reverse
                        {:forward {:forward true :reverse false}
                         :right {:right true}
                         :left {:left true}
                         :stop {:reverse false}}
                        ;; reverse-left
                        {:forward {:forward true :reverse false :left false}
                         :reverse {:left false}
                         :right {:right true :left false}
                         :stop {:reverse false :left false}}
                        ;; forward-left
                        {:forward :{left false}
                         :reverse {:forward false :left false}
                         :right {:left false :right true}
                         :stop {:forward false :left false}}])

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
  (doseq [i [1 2]]
     (motor-forward i speed))
  (doseq [i [3 4]]
    (motor-forward i (/ speed 2))))

(defn drive-right [speed]
  (change-state :right speed)
  (doseq [i [1 2]]
    (motor-forward i (/ speed 2)))
  (doseq [i [3 4]]
    (motor-forward i speed)))
