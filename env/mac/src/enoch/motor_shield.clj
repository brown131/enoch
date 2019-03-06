(ns enoch.motor-shield
  "Dummy API for Maker-Sphere Motor-Shield v1 to be used on platforms other than the Raspberry Pi.")

(defn gpio-shutdown [] (println "gpio shutdown"))

(defn arrow-init [id] (println "arrow init" id))

(defn arrow-on [id] (println "arrow on" id))

(defn arrow-off [id] (println "arrow off" id))

(defn motor-init [id] (println "motor init" id))

(defn motor-forward [id] (println "motor forward" id))

(defn motor-reverse [id] (println "motor reverse" id))

(defn motor-stop [id] (println "motor stop" id))

(defn servo-init [id] (println "servo init" id))

(defn servo-rotate [id wait range-fn]
  (println "servo rotate" id wait (first (range-fn)) (last (range-fn))
           (- (second (range-fn)) (first (range-fn)))))

(defn servo-stop [id] (println "servo stop" id))

(defn ultrasonic-init [id] (println "ultrasonic init" id))

(defn ultrasonic-check [id] (println "ultrasonic check" id))

(defn ultrasonic-stop [id] (println "ultrasonic stop" id))