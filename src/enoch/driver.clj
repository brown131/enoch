(ns enoch.driver "Literally drives the car."
  (:require [taoensso.timbre :as log]
            [enoch.motor-shield :refer :all]))

(log/refer-timbre)

(def car-state (atom {:mode :stop :direction :straight :speed 0}))

(defn change-motors []
  (when-not (= (:mode @car-state) :stop)
    (doseq [id [:front-right :back-right]]
      ((if (= (:mode @car-state) :forward) motor-forward motor-reverse) id
        (if (= (:direction @car-state) :right) (/ (:speed @car-state) 2) (:speed @car-state))))
    (doseq [id [:front-left :back-left]]
      ((if (= (:mode @car-state) :forward) motor-forward motor-reverse) id
        (if (= (:direction @car-state) :left) (/ (:speed @car-state) 2) (:speed @car-state))))))

(defn stop-motors []
  (doseq [id [:front-left :front-right :back-left :back-right]]
    (motor-stop id)))

(defn drive-forward []
  (when-not (and (= (:mode @car-state) :forward) (= (:direction @car-state) :straight))
    (when (= (:mode @car-state) :reverse)
      (arrow-off :reverse)
      (stop-motors)
      (Thread/sleep 333))
    (when-not (= (:direction @car-state) :straight)
      (arrow-off (:direction @car-state)))
    (when-not (= (:mode @car-state) :forward)
      (arrow-on :forward))
    (change-motors)
    (swap! car-state assoc :mode :forward :direction :straight)))

(defn drive-reverse []
  (when-not (and (= (:mode @car-state) :reverse) (= (:direction @car-state) :straight))
    (when (= (:mode @car-state) :forward)
      (arrow-off :forward)
      (stop-motors)
      (Thread/sleep 333))
    (when-not (= (:direction @car-state) :straight)
      (arrow-off (:direction @car-state)))
    (when-not (= (:mode @car-state) :reverse)
      (arrow-on :reverse))
    (change-motors)
  (swap! car-state assoc :mode :reverse :direction :straight)))

(defn drive-right []
  (when-not (= (:direction :right))
    (when (= (:direction @car-state) :left)
      (arrow-off :left))
    (when-not (= (:direction @car-state) :right)
      (arrow-on :right))
    (when (= (:mode @car-state) :stop)
      (swap! car-state assoc :mode :forward))
    (change-motors)
    (swap! car-state assoc :direction :right)))

(defn drive-left []
  (when-not (= (:direction :left))
    (when (= (:direction @car-state) :right)
      (arrow-off :right))
    (when-not (= (:direction @car-state) :left)
      (arrow-on :left))
    (when (= (:mode @car-state) :stop)
      (swap! car-state assoc :mode :forward))
    (change-motors)
    (swap! car-state assoc :direction :left)))

(defn drive-faster []
  (when (< (:speed @car-state) 100)
    (swap! car-state update :speed #(+ % 10))
    (change-motors)))

(defn drive-slower []
  (when (> (:speed @car-state) 100 0)
    (swap! car-state update :speed #(- % 10))
    (change-motors)))

(defn drive-stop []
  (when-not (= (:mode @car-state) :stop)
    (arrow-off (:mode @car-state))
    (when-not (= (:direction @car-state) :straight)
      (arrow-off (:direction @car-state)))
    (stop-motors)
    (swap! car-state assoc :mode :stop :direction :straight :speed 0)))
