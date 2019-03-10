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

(defn send-speech-config-msg []
  "Assemble the payload for the speech.config message."
  (let [os-name (:out (sh "uname" "-sv"))
        payload {"context" {"system" {"version" "5.4"}
                            "os" {"platform" (first (s/split os-name))
                                  "name" os-name
                                  "version" (s/join #" " (rest (s/split os-name)))}
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
          start (generate-timestamp)]
      (try
        ;; Request a WebSocket connection to the speech API.
        (reset! websocket (ws/connect url :header headers
                                      :on-connect (fn [s]
                                                    ;; Record the Connection metric telemetry data.
                                                    (swap! metrics conj {"Name" "Connection"
                                                                         "Id" connection-id
                                                                         "Start" start
                                                                         "End" (generate-timestamp)})

                                                    ;; Send the speech.config message.
                                                    (send-speech-config-msg))))
        (catch Exception e
          (log/error e "Handshake error"))))
    (log/error "Invalid recognition mode.")))

(defn disconnect-speech-api []
  (ws/close @websocket))

;;; Perform the sending and receiving via the WebSocket concurrently.
;(defn/a speech-to-text [self audio-file-path]
;        (setv sending-task (asyncio.ensure_future (self.send-audio-msg audio-file-path)))
;        (setv receiving-task (asyncio.ensure_future (self.process-response)))
;
;        ;; Wait for both the tasks to complete.
;        (await (asyncio.wait [sending-task receiving-task] :return_when asyncio.ALL_COMPLETED))
;
;        (return self.phrase))

(defn go-connect-speech-api [] )

(defn go-connect-speech-api-response [] )
