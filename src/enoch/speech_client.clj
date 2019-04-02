(ns enoch.speech-client
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [clj-http.client :as client]
            [taoensso.timbre :as log]
            [enoch.config :refer [config-properties secret-properties]]))

(log/refer-timbre)

(def commands {:en {"forward"    :forward
                    "left"       :left
           	    "right"      :right
                    "backward"   :reverse
                    "faster"     :faster
                    "slower"     :slower
                    "stop"       :stop               
                    "shut down"  :shutdown}
	       :de {"gerade aus" :forward
	            "geradeaus"  :forward
	            "link"       :left
		    "recht"      :right
		    "rückwärts"  :reverse
		    "schneller"  :faster
		    "langsamer"  :slower
		    "halt an"    :stop
		    "fahr ab"    :shutdown}})
  
(defn clean-text [text]
  (-> text s/lower-case (s/replace #"[,;\"\.\?!]" "")))

(defn go-process-stt-response [response-chan command-chan speaker-chan]
  (async/go-loop [response (async/<! response-chan)]
    (when response
      (log/debug "<<" response)
      (if (= (:status response) 200)
        (let [body (json/read-str (:body response))
	      lang-cmds (get commands (:language @config-properties))]
          (case (s/trim (get body "RecognitionStatus"))
            "Success" (when-let [text (clean-text (case (:response-format @config-properties)
                                                    "simple" (get body "DisplayText")
                                                    "detailed" (get (first (get body "NBest")) "Display")
                                                    (log/info "Unexpected response format.")))]
	  		(log/debug "text:" text)			    
                        (if-let [command (first (filter #(s/index-of text %) (keys lang-cmds)))]
                          (async/put! command-chan (get lang-cmds command)
                          (async/put! speaker-chan (str (if (= (:language @config-properties) :de)
			                                    "Ich verstehe nicht "
			                                    "I do not understand") text)))))
            "InitialSilenceTimeout" (log/debug "Silence time-out")
            "Error" (log/error "Error response" response)
            (log/error "Unrecognized status" (get body "RecognitionStatus"))))
        (log/error "HTTP error response" response))
      (recur (async/<! response-chan)))))

(defn go-send-stt-request "Send audio from the audio channel."
  [microphone-chan response-chan shutdown-chan]
  (async/go-loop []
    ;; Wait for either the microphone or a shutdown.
    (let [[buffer ch] (async/alts!! [microphone-chan shutdown-chan])]
      (log/debug "buffer" (count buffer))
      (when (= ch shutdown-chan)
        (log/info "Send STT request shutdown"))
      (when (not= ch shutdown-chan)
        (try
          (let [lang (if (= (:language @config-properties) :de) "de-DE" "en-US")
	        url (format (:stt-request-url @config-properties) lang
                            (:response-format@config-properties) )
                headers {"Ocp-Apim-Subscription-Key" (:speech-recognition-api-key @secret-properties)
                         "Accept" "eapplication/json;text/xml"}]
            (log/debug ">>" url headers (count buffer))
            (client/post url {:headers headers
                              :content-type "audio/wav; codecs=audio/pcm; samplerate=16000"
                              :body (byte-array buffer)
                              :async? true}
                         #(async/put! response-chan %)
                         #(log/error "STT request error" %)))
          (catch Exception e
            (log/error e)))
        (recur)))))

