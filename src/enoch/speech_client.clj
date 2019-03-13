(ns enoch.speech-client
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [gniazdo.core :as ws]
            [taoensso.timbre :as log]
            [enoch.config :refer [config-properties secret-properties]])
  (:import [java.util UUID]))

(log/refer-timbre)

(def language "en-US")
(def response-format "simple")
(def recognition-mode :interactive)

(def auth-token (atom nil))
(def websocket (atom nil))
(def metrics (atom []))

(defn generate-id []
  (s/replace (str (UUID/randomUUID)) #"-" ""))

(defn generate-timestamp "Format is \"2019-03-06T21:34:01.457Z\""
  []
  (f/unparse (f/formatters :date-time) (t/now)))

(defn do-obtain-auth-token "Has a TTL of 10 minutes, minus 10 seconds."
  [shutdown-chan]
  (async/thread
    (loop []
      (try
        (let [url (:auth-token-url @config-properties)
              headers {:content-type "application/x-www-form-urlencoded"
                       :headers {"Ocp-Apim-Subscription-Key" (:speech-recognition-api-key @secret-properties)}}
              response (client/post url headers)]

          (when (= (:status response) 200)
            (reset! auth-token (:body response)))
          (when (or (= (:status response) 403) (= (:status response) 401))
            (log/error "Error: access denied. Please, make sure you provided a valid API key.")))
          (catch Exception e
            (log/error e)))

      ;; Wait for either a shutdown or a time-out.
      (let [[_ ch] (async/alts!! [shutdown-chan (async/timeout (* 590 1000))])]
        (when (not= ch shutdown-chan)
          (recur))))))

(defn send-speech-config-msg
  []
  (let [os-name (:out (sh "uname" "-sv"))
        payload {"context" {"system" {"version" "5.4"}
                            "os" {"platform" (first (s/split os-name #" "))
                                  "name" os-name
                                  "version" (s/join " " (rest (s/split os-name #" ")))}
                            "device" {"manufacturer" "brown131@yahoo.com"
                                      "model" "Enoch"
                                      "version" "0.1.0"}}}
        ;; Assemble the header for the speech.config message.
        msg (str "Path: speech.config\r\n"
                 "Content-Type: application/json; charset=utf-8\r\n"
                 "X-Timestamp:" (generate-timestamp) "\r\n"
                 ;; Append the body of the message.
                 "\r\n" (json/write-str payload))]
    (log/debug ">>" msg)
    (ws/send-msg @websocket msg)))

(defn process-connect [connection-id start-time response]
  (log/info "Speech API client connected.")

  ;; Record the Connection metric telemetry data.
  (swap! metrics conj {"Name" "Connection"
                       "Id" connection-id
                       "Start" start-time
                       "End" (generate-timestamp)})

  (send-speech-config-msg))

(defn process-response [response]
  )

(defn connect-speech-api
  "Determine the endpoint based on the selected recognition mode."
  []
  ;; Wait for an auth token.
  (while (nil? @auth-token)
    (Thread/sleep 200))

  (if-let [endpoint (get-in @config-properties [:endpoints recognition-mode])]
    ;; Assemble the URL and the headers for the connection request.
    (let [connection-id (generate-id)
          url (format "%s?language=%s&format=%s" endpoint language response-format)
          headers {"Authorization" (str "Bearer " @auth-token)
                   "X-ConnectionId" connection-id}
          start-time (generate-timestamp)]
      (try
        ;; Request websocket connection to the STT API.
        (println url)
        (reset! websocket (ws/connect url :headers headers
                                      :on-error #(log/error "Error connectiing websocket" %)
                                      :on-connect (partial process-connect connection-id start-time)
                                      :on-receive process-response))
        (catch Exception e
          (log/error e "Handshake error"))))
    (log/error "Invalid recognition mode.")))

(defn disconnect-speech-api []
  (ws/close @websocket))

#_(defn send-audio-msg [audio-file-path]
  (with [f-audio (open audio-file-path "rb")]
        (setv num-chunks 0)
        (while True
          ;; Read the audio file in small consecutive chunks.
          (setv audio-chunk (.read f-audio self.chunk-size))
          (unless audio-chunk
                  (break))
          (setv num-chunks (inc num-chunks))

          ;; Assemble the header for the binary audio message.
          (setv msg (ctr "Path: audio\r\n"
                         "Content-Type: audio/x-wav\r\n"
                         "X-RequestId: " (bytearray self.request-id "ascii") b"\r\n"
                         "X-Timestamp: " (bytearray (utils.generate-timestamp) "ascii") b"\r\n"))
          ;; Prepend the length of the header in 2-byte big-endian format.
          (setv msg (+ (.to-bytes (len msg) 2 :byteorder "big") msg))
          ;; Append the body of the message.
          (setv msg (+ msg b"\r\n" audio-chunk))

          ;; DEBUG PRINT
          ;; (print ">>" msg)
          ;; (.flush sys.stdout)

          (try
            (await (.send self.ws msg))
            ;; DEBUG CONCURRENCY
            ;; (await (asyncio.sleep 0.1))
            (except [e websockets.exceptions.ConnectionClosed]
                    (print (.format "Connection closed: {0}" e)))))))

(defn go-speech-to-text "Read audio from the channel and send it to the STT service."
  [audio-chan]
  )