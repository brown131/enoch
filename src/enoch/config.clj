(ns enoch.config
  (:require [clojure.java.io :as io]))

(defn read-config [file-name]
  (-> file-name
      io/resource
      slurp
      read-string
      delay))

(def config-properties (read-config "application.edn"))

(def secret-properties (read-config "secret.edn"))
