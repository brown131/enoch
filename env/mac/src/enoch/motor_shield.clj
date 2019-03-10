(ns enoch.motor-shield
  "Dummy API for Maker-Sphere Motor-Shield v1 to be used on platforms other than the Raspberry Pi."
  (:require [taoensso.timbre :as log]))

(log/refer-timbre)

(defn gpio-shutdown [] (log/info "gpio shutdown"))

(defn arrow-init [id] (log/info "arrow init" id))

(defn arrow-on [id] (log/info "arrow on" id))

(defn arrow-off [id] (log/info "arrow off" id))

(defn motor-init [id] (log/info "motor init" id))

(defn motor-forward [id speed] (log/info "motor forward" id speed))

(defn motor-reverse [id speed] (log/info "motor reverse" id speed))

(defn motor-stop [id] (log/info "motor stop" id))

(defn servo-init [id] (log/info "servo init" id))

(defn servo-rotate [id wait range-fn]
  (log/info "servo rotate" id wait (first (range-fn)) (last (range-fn))
           (- (second (range-fn)) (first (range-fn)))))

(defn servo-stop [id] (log/info "servo stop" id))

(defn ultrasonic-init [id] (log/info "ultrasonic init" id))

(defn ultrasonic-check [id] (log/info "ultrasonic check" id))

(defn ultrasonic-stop [id] (log/info "ultrasonic stop" id))