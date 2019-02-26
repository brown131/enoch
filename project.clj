(defproject enoch "0.1.0"
  :description "Enoch - Raspberry Pi 'Bot"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["csspeech" "https://csspeechstorage.blob.core.windows.net/maven/"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.bidib.com.pi4j/pi4j-core "1.2.M1"]
                 [org.bidib.com.pi4j/pi4j-device "1.2.M1"]
                 [org.bidib.com.pi4j/pi4j-gpio-extension "1.2.M1"]]
  :main enoch.core
  :aot :all
  )
