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
(def metrics (atom nil))
(def received-messages (atom nil))

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

(defn record-telemetry [request-id response-path]
  (swap! received-messages assoc-in [request-id response-path]
         (if-let [msg (get @received-messages response-path)]
           (conj (if (seq? msg) msg [msg]) (generate-timestamp))
           (generate-timestamp))))

(defn send-telemetry-msg
  [request-id]
  (let [payload (conj {"ReceivedMessages" (get @received-messages request-id)}
                      (when @metrics ["Metrics" @metrics]))
        ;; Assemble the header for the speech.config message.
        msg (str "Path: telemetry\r\n"
                 "Content-Type: application/json; charset=utf-8\r\n"
                 "X-RequestId:" request-id "\r\n"
                 "X-Timestamp:" (generate-timestamp) "\r\n"
                 ;; Append the body of the message.
                 "\r\n" (json/write-str payload))]
    (log/debug ">>" msg)
    (ws/send-msg @websocket msg)
    (reset! metrics nil)))

(defn parse-response [response]
  (let [lines (s/split response #"\r\n")]
    (loop [lines lines
           headers {}]
      (if (empty? (first lines))
        [headers (json/read-str (s/join (rest lines)))]
        (recur (rest lines) (conj headers (s/split (first lines) #":")))))))

(defn process-response [response]
  (log/debug "<<" response)
  (let [[headers body] (parse-response response)]
    (let [request-id (get headers "X-RequestId")
          response-path (get headers "Path")]
      (record-telemetry request-id response-path)
      (case response-path
        "turn.start" (when (get body "Error") (log/error "Error response" response))
        "speech.startDetected" (when (get body "Error") (log/error "Error response" response))
        "speech.hypthosis" (if (get body "Error")
                             (log/error "Error response" response)
                             (log/info "Current hypthosis" (get body "Text")))
        "speech.phrase" (case (get body "RecognitionStatus")
                          "Success" (case response-format
                                      "simple" (log/info "Simple" (get body "DisplayText"))
                                      "detailed" (log/info "Detailed" (get (first (get body "NBest")) "Display"))
                                      (log/info "Unexpected response format."))
                          "Error" (log/error "Error response" response))
        "speech.endDetected" (when (get body "Error") (log/error "Error response" response))
        "turn.end" (if (get body "Error")
                     (log/error "Error response" response)
                     (swap! received-messages dissoc request-id))
        (log/error "Unexpected response type (Path header)." response-path))
      (send-telemetry-msg request-id))))

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
        (reset! websocket (ws/connect url :headers headers
                                      :on-close #(log/info "Web socket connection closed" %1 %2)
                                      :on-connect (partial process-connect connection-id start-time)
                                      :on-error #(log/error "Web socket error" %)
                                      :on-receive process-response
                                      :on-binary (constantly (log/error "Unexpected binary response."))))
        (catch Exception e
          (log/error e "Handshake error"))))
    (log/error "Invalid recognition mode.")))

(defn disconnect-speech-api []
  (ws/close @websocket)
  (reset! metrics nil))

(defn do-send-audio-msg "Send audio from the audio channel."
  [audio-chan shutdown-chan]
  (async/thread
    (loop []
      ;; Wait for either audio or a shutdown.
      (let [[buffer ch] (async/alts!! [audio-chan shutdown-chan])]
        (log/debug "buffer" (count buffer) "ch" ch)
        (when (= ch shutdown-chan)
          (log/info "do-send-audio-msg shutdown"))
        (when (not= ch shutdown-chan)
          (try
            (let [request-id (generate-id)
                  headers (map byte (str "Path: audio\r\n"
                                         "Content-Type: audio/x-wav\r\n"
                                         "X-RequestId: " request-id "\r\n"
                                         "X-Timestamp: " (generate-timestamp) "\r\n"))
                  headers-len (short (count headers))
                  msg (bytes (byte-array (concat [(quot headers-len 256) (mod headers-len 256)]
                                                 headers buffer)))]
              (log/debug ">>" headers (count buffer))
              (ws/send-msg @websocket msg))
            (catch Exception e
              (log/error e)))
          (recur))))))
