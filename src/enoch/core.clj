(ns enoch.core
  (:require [enoch.motor-shield :refer :all])
  (:gen-class))

(defn -main [& args]
  (println "Hello, World!")
  (arrow-init 1)
  (arrow-on 1)
  (Thread/sleep 2000)
  (arrow-off 1)
  (gpio-shutdown))
