(ns enoch.config
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :refer [rotor-appender]]))

(log/refer-timbre)

(defn- read-config [file-name]
  (-> file-name
      io/resource
      slurp
      read-string
      eval
      delay))

(def logger-config (read-config "logger.edn"))

(def config-properties (read-config "application.edn"))

(def secret-properties (read-config "secret.edn"))
