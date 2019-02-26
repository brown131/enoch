(ns enoch.core
  (:require [pimotor.core :refer :all])
  (:gen-class))

(defn -main [& args]
  (println "Hello, World!")
  (make-arrow 1)
  (arrow-on 1)
  (Thread/sleep 2000)
  (arrow-off 1)
  (gpio-shutdown))
