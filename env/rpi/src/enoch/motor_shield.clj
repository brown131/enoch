(ns enoch.motor-shield
  "API for SB Components Maker-Sphere Motor-Shield v1 to be used on the Raspberry Pi."
  (:require [taoensso.timbre :as log])
  (:import [com.pi4j.wiringpi Gpio SoftPwm]
           [com.pi4j.io.gpio GpioFactory RaspiPin PinState Pin]
           [com.pi4j.component.servo ServoDriver ServoProvider]
           [com.pi4j.component.servo.impl RPIServoBlasterProvider]))

(log/refer-timbre)

(def gpio (GpioFactory/getInstance))

(def motor-pins {:front-left {:enable RaspiPin/GPIO_12 :forward RaspiPin/GPIO_13 :reverse RaspiPin/GPIO_14}
                 :front-right {:enable RaspiPin/GPIO_00 :forward RaspiPin/GPIO_03 :reverse RaspiPin/GPIO_02}
                 :back-left {:enable RaspiPin/GPIO_26 :forward RaspiPin/GPIO_10 :reverse RaspiPin/GPIO_11}
                 :back-right {:enable RaspiPin/GPIO_06 :forward RaspiPin/GPIO_04 :reverse RaspiPin/GPIO_05}})

(def arrow-pins {:front RaspiPin/GPIO_25
                 :back  RaspiPin/GPIO_23
                 :left  RaspiPin/GPIO_24
                 :right RaspiPin/GPIO_27})

(def ultrasonic-pins {:front {:echo RaspiPin/GPIO_22 :trigger RaspiPin/GPIO_21}})

(def motors (atom nil))
(def arrows (atom nil))
(def servos (atom nil))
(def ultrasonic (atom nil))
(def motor-test (atom false))

(defn gpio-shutdown "Deallocate GPIO resources."
  [] (.shutdown gpio))


;;; Arrow


(defn arrow-init "Initialize an arrow with an id of :front, :back, :left, or :right."
  [id]
  (when-not (id @arrows)
    (swap! arrows assoc id (.provisionDigitalOutputPin gpio (id arrow-pins) (str "Arrow" id) PinState/LOW))))

(defn arrow-on "Lights up an arrow by id."
  [id]
  (arrow-init id)
  (.high (id @arrows)))

(defn arrow-off "Turns off an arrow by id."
  [id]
  (arrow-init id)
  (.low (id @arrows)))


;;; Motor


(defn motor-init "Initialize a motor with an id of :front-left :front-right :back-left :back-right."
  [id]
  (when-not (id @motors)
    (swap! motors assoc id {:enable (.provisionDigitalOutputPin gpio (get-in motor-pins [id :enable]) (str "MotorEnable" id))
                            :forward (.provisionDigitalOutputPin gpio (get-in motor-pins [id :forward]) (str "MotorForward" id))
                            :reverse (.provisionDigitalOutputPin gpio (get-in motor-pins [id :reverse]) (str "MotorReverse" id))}))
    (SoftPwm/softPwmCreate (.getAddress (get-in motor-pins [id :reverse])) 0 100))

(defn motor-forward "Start the motor turning in its configured \"forward\" direction."
  [id speed]
  (log/info "motor forward" id speed)
  (if @motor-test
    (arrow-on id)
    (do
      (motor-init id)
      (.high (get-in @motors [id :enable]))
      (.high (get-in @motors [id :forward]))
      (.low (get-in @motors [id :reverse]))
      (SoftPwm/softPwmWrite (.getAddress (get-in motor-pins [id :forward])) speed)
      (SoftPwm/softPwmWrite (.getAddress (get-in motor-pins [id :reverse])) 0))))

(defn motor-reverse "Start the motor turning in its configured \"reverse\" direction."
  [id speed]
  (log/info "motor reverse" id speed)
  (if @motor-test
    (arrow-off id)
    (do
      (motor-init id)
      (.high (get-in @motors [id :enable]))
      (.low (get-in @motors [id :forward]))
      (.high (get-in @motors [id :reverse]))
      (SoftPwm/softPwmWrite (.getAddress (get-in motor-pins [id :forward])) 0)
      (SoftPwm/softPwmWrite (.getAddress (get-in motor-pins [id :reverse])) speed))))

(defn motor-stop "Stop power to the motor."
  [id]
  (log/info "motor stop" id)
  (if @motor-test
    (arrow-off id)
    (do
      (motor-init id)
      (SoftPwm/softPwmWrite (.getAddress (get-in motor-pins [id :forward])) 0)
      (SoftPwm/softPwmWrite (.getAddress (get-in motor-pins [id :reverse])) 0)
      (.low (get-in @motors [id :forward]))
      (.low (get-in @motors [id :reverse]))
      (.low (get-in @motors [id :enable])))))


;;; Servo


(defn servo-init "Initialize servo with an id of :hortizontal or :vertical."
  [id]
  (when-not (get @servos id)
    (let [pin (if (= id :horizontal) 0 1)
          servo-provider (RPIServoBlasterProvider.)]
      (swap! servos assoc id (.getServoDriver servo-provider (.get (.getDefinedServoPins servo-provider) pin))))))

(defn servo-rotate
  "Rotate the servo by the range. Range is from 0 to 100.
   Example: (servo-rotate 1 10 #(range 90 30 -1))"
  [id wait range-fn]
  (servo-init id)
  (doseq [i (range-fn)]
    (.setServoPulseWidth (id @servos) i)
    (Thread/sleep wait)))

(defn servo-stop
  "Stop the servo motor."
  [id]
  (.setServoPulseWidth (id @servos) 0))


;;; Ultrasonic


(defn ultrasonic-init "Initialize a sensor by id, currently only :front."
  [id]
  (when-not (id @ultrasonic)
    (swap! ultrasonic assoc id {:last-read 0
                                :echo-pin (.provisionDigitalInputPin gpio (get-in ultrasonic-pins [id :echo])
			                                             (str "UltrasonicEcho" id))
  			        :trigger-pin (.provisionDigitalOutputPin gpio (get-in ultrasonic-pins [id :trigger])
			                                                 (str "UltrasonicTrigger" id) PinState/LOW)})))

(defn ultrasonic-check
  "Check if the distance measured using ultrasonic sensor."
  [id]
  (ultrasonic-init id)
    
  (Thread/sleep 333)
  (.high (get-in @ultrasonic [id :trigger-pin]))
  (Thread/sleep 0 10000)
  (.low (get-in @ultrasonic [id :trigger-pin]))
  
  (let [timed-out (atom false)
        too-long #(reset! timed-out (> (- (System/currentTimeMillis) %) 1000))
        start (let [started (System/currentTimeMillis)]
                (while (and (.isLow (get-in @ultrasonic [id :echo-pin]))
		            (not (too-long started))))
		(System/nanoTime))
        end   (let [started (System/currentTimeMillis)]
	        (while (and (not @timed-out)
		            (.isHigh (get-in @ultrasonic [id :echo-pin]))
		            (not (too-long started))))
                (System/nanoTime))]
      (if @timed-out
        (do (log/info "Ultrasound timed out")
	    :timed-out)
	;; 0.0000174 * 38400 meters/sec / 2 [half the distance there and back] / 1E9 
        (* (double (- end start)) 0.0000174))))
 
(defn ultrasonic-stop [id]
  (.low (get-in @ultrasonic [id :trigger-pin])))
