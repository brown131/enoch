(ns enoch.driver "Literally drives the car."
  (:require [taoensso.timbre :as log]
            [enoch.config :refer [config-properties]]
            [enoch.motor-shield :refer :all]))

(log/refer-timbre)

(def car-state (atom {:mode :stop :direction :straight :speed 0}))

(defn change-motors []
  (log/info "car state" (pr-str @car-state))
  (let [{:keys [mode direction speed]} @car-state]
    (when-not (= mode :stop)
      (doseq [id [:front-right :back-right]]
        ((if (= mode :forward) motor-forward motor-reverse) id
          (if (= direction :right) (/ speed 2) speed)))
      (doseq [id [:front-left :back-left]]
        ((if (= mode :forward) motor-forward motor-reverse) id
          (if (= direction :left) (/ speed 2) speed))))))

(defn stop-motors []
  (doseq [id [:front-left :front-right :back-left :back-right]]
    (motor-stop id)))

(defn drive-forward []
  (let [{:keys [mode direction speed]} @car-state]
    (when-not (and (= mode :forward) (= direction :straight))
      (when (= mode :reverse)
        (arrow-off :back)
      	(stop-motors)
        (Thread/sleep 333))
      (when-not (= direction :straight)
        (arrow-off direction))
      (when-not (= mode :forward)
        (arrow-on :front))
      (swap! car-state assoc :mode :forward :direction :straight
             :speed (if (pos? speed) speed (:default-speed @config-properties)))
      (change-motors))))

(defn drive-reverse []
  (let [{:keys [mode direction speed]} @car-state]
    (when-not (and (= mode :reverse) (= direction :straight))
      (when (= mode :forward)
        (arrow-off :front)
        (stop-motors)
        (Thread/sleep 333))
      (when-not (= direction :straight)
        (arrow-off direction))
      (when-not (= mode :reverse)
        (arrow-on :back))
      (swap! car-state assoc :mode :reverse :direction :straight
           :speed (if (pos? speed) speed (:default-speed @config-properties)))
      (change-motors))))

(defn drive-right []
  (let [{:keys [mode direction speed]} @car-state]
    (when-not (= direction :right)
      (when (= direction :left)
        (arrow-off :left))
      (when-not (= direction :right)
        (arrow-on :right))
      (when (= mode :stop)
        (swap! car-state assoc :mode :forward))
      (swap! car-state assoc :direction :right)
      (change-motors))))

(defn drive-left []
  (let [{:keys [mode direction speed]} @car-state]
    (when-not (= direction :left)
      (when (= direction :right)
        (arrow-off :right))
      (when-not (= direction :left)
        (arrow-on :left))
      (when (= mode :stop)
        (swap! car-state assoc :mode :forward))
      (swap! car-state assoc :direction :left)
      (change-motors))))

(defn drive-faster []
  (when (< (:speed @car-state) 100)
    (swap! car-state update :speed #(+ % 10))
    (change-motors)))

(defn drive-slower []
  (when (> (:speed @car-state) 0)
    (swap! car-state update :speed #(- % 10))
    (change-motors)))

(defn drive-stop []
  (let [{:keys [mode direction speed]} @car-state]
    (when-not (= mode :stop)
      (arrow-off (if (= mode :forward) :front :back))
      (when-not (= direction :straight)
        (arrow-off direction))
      (swap! car-state assoc :mode :stop :direction :straight :speed 0)
      (stop-motors))))
