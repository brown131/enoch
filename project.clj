(defproject enoch "0.1.0"
  :description "Enoch - Raspberry Pi 'Bot"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.9.1"]
                 [clj-time "0.15.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [stylefruits/gniazdo "1.1.1"]]
  :main enoch.core
  :aot :all
  :resource-paths ["resources"]
  :profiles {:rpi {:source-paths ["src" "env/rpi/src"]
                   :test-paths ["test" "env/rpi/test"]
                   :dependencies [[org.bidib.com.pi4j/pi4j-core "1.2.M1"]
                                  [org.bidib.com.pi4j/pi4j-device "1.2.M1"]
                                  [org.bidib.com.pi4j/pi4j-gpio-extension "1.2.M1"]]}
             :mac {:source-paths ["src" "env/mac/src"]
                   :test-paths ["test" "env/mac/test"]}})
