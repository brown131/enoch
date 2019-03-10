(ns enoch.speech-client
  (:require [clojure.string :as s]
            [taoensso.timbre :as log]
            [enoch.config :refer [config-properties secret-properties]])
  (:import [java.util UUID]))

(log/refer-timbre)

(def language "en-US")
(def response-format "simple")
(def recognition-mode :interactive)

(def endpoints {:interactive "wss://speech.platform.bing.com/speech/recognition/interactive/cognitiveservices/v1"
                :conversation "wss://speech.platform.bing.com/speech/recognition/conversation/cognitiveservices/v1"
                :dictation "wss://speech.platform.bing.com/speech/recognition/dictation/cognitiveservices/v1"})

(defn generate-id []
  (s/replace (str (UUID/randomUUID)) #"-" ""))

;(defn generate-timestamp "Format is \"2019-03-06T21:34:01.457Z\"" []
;  (+ (cut (s/replace (str (datetime.datetime.now)) " " "T") None (- 3)) "Z"))
;
;(defn obtain-auth-token "Has TTL of 10 minutes."
;  [api-key]
;
;  (let [url "https://api.cognitive.microsoft.com/sts/v1.0/issueToken"
;        headers {"Content-type" "application/x-www-form-urlencoded"
;                 "Content-Length" "0"
;                 "Ocp-Apim-Subscription-Key" (:speech-recognition-api-key @secret-properties)}]
;    (setv response (requests.post url :headers headers))
;
;  ;; DEBUG PRINT
;  ;(print (get response.headers "content-type"))
;  ;(print response.text)
;
;  (if (= response.status-code 200)
;    (setv data response.text)
;    (if (or (== (.status-code response) 403) (.status-code response 401))
;      (do (print "Error: access denied. Please, make sure you provided a valid API key.")
;          (exit))
;      (.raise-for-status response)))
;  data)
;
;(defn connect-to-speech-api
;  "Determine the endpoint based on the selected recognition mode."
;  [language response-format recognition-mode]
;  (let [endpoint (get endpoints recognition-mode)
;        connection-id (generate-id)]
;    (if-not endpoint
;      (print "Error: invalid recognition mode.")
;      ;; Assemble the URL and the headers for the connection request.
;      (let [url (format "%s?language=%s&format=%s" endpoint language response-format)
;            headers {"Authorization" (+ "Bearer " auth-token)
;                     "X-ConnectionId" connection-id}]
;
;
;
;(setv headers {"Authorization" (+ "Bearer " self.auth-token)
;               "X-ConnectionId" self.connection-id})
;
;;; Record the Connection metric telemetry data.
;(.append self.metrics {"Name" "Connection"
;                       "Id" self.connection-id
;                       "Start" (utils.generate-timestamp)})
;
;(try
;  ;; Request a WebSocket connection to the speech API.
;  (setv self.ws (await (websockets.client.connect url :extra_headers headers)))
;  (except [err websockets.exceptions.InvalidHandshake]
;          (print (format "Handshake error: {0}" err))
;          (return)))
;;; TODO: Add Connection failure telemetry for error cases.
;
;;; Record the Connection metric telemetry data.
;(setv (get self.metrics -1 "End") (utils.generate_timestamp))
;;; Send the speech.config message.
;(await (self.send-speech-config-msg)))
;
;(defn/a disconnect [self]
;        (await (.close self.ws)))
;
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
