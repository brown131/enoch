(ns enoch.speech-client
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [clj-http.client :as client]
            [taoensso.timbre :as log]
            [enoch.config :refer [config-properties secret-properties]]))

(log/refer-timbre)

(def language "en-US")
(def response-format "simple")

(defn go-process-response [message-chan]
  (async/go-loop [response (async/<! message-chan)]
    (when response
      (log/debug "<<" response)
      (if (= (:status response) 200)
        (let [body (json/read-str (:body response))]
          (case (get body "RecognitionStatus")
            "Success" (case response-format
                        "simple" (log/info "Simple" (get body "DisplayText"))
                        "detailed" (log/info "Detailed" (get (first (get body "NBest")) "Display"))
                        (log/info "Unexpected response format."))
            "InitialSilenceTimeout" nil
            "Error" (log/error "Error response" response)
            (log/error "Unrecognized status [" (get body "RecognitionStatus") "]")))
        (log/error "HTTP error response" response))
      (recur (async/<! message-chan)))))

(defn go-send-stt-request "Send audio from the audio channel."
  [microphone-chan message-chan shutdown-chan]
  (async/go-loop []
    ;; Wait for either audio or a shutdown.
    (let [[buffer ch] (async/alts!! [microphone-chan shutdown-chan])]
      (log/debug "buffer" (count buffer))
      (when (= ch shutdown-chan)
        (log/info "Send STT request shutdown"))
      (when (not= ch shutdown-chan)
        (try
          (let [url (format (:stt-request-url @config-properties) language response-format)
                headers {"Ocp-Apim-Subscription-Key" (:speech-recognition-api-key @secret-properties)
                         "Accept" "eapplication/json;text/xml"}]
            (log/debug ">>" url headers (count buffer))
            (client/post url {:headers headers
                              :content-type "audio/wav; codecs=audio/pcm; samplerate=16000"
                              :body (byte-array buffer)
                              :async? true}
                         #(async/put! message-chan %)
                         #(log/error "Web socket error" %)))
          (catch Exception e
            (log/error e)))
        (recur)))))

