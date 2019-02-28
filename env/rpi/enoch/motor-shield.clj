(ns enoch.motor-shield
  (:import [com.pi4j.wiringpi Gpio]
           [com.pi4j.io.gpio GpioFactory RaspiPin PinState]
           [com.pi4j.component.servo.impl RPIServoBlasterProvider]))

(def gpio (GpioFactory/getInstance))

(def pwm (.provisionPwmOutputPin gpio RaspiPin/GPIO_01))

(def motor-pins {1 {:enable RaspiPin/GPIO_26 :forward RaspiPin/GPIO_10 :reverse RaspiPin/GPIO_11}
                 2 {:enable RaspiPin/GPIO_12 :forward RaspiPin/GPIO_13 :reverse RaspiPin/GPIO_14}
		 3 {:enable RaspiPin/GPIO_06 :forward RaspiPin/GPIO_04 :reverse RaspiPin/GPIO_05}
		 4 {:enable RaspiPin/GPIO_00 :forward RaspiPin/GPIO_03 :reverse RaspiPin/GPIO_02}})
	 
(def arrow-pins {1 RaspiPin/GPIO_23
                 2 RaspiPin/GPIO_24
                 3 RaspiPin/GPIO_25
                 4 RaspiPin/GPIO_27})

(def servo-pins {1 RaspPin/GPIO_07
                 2 RaspPin/GPIO_01})

(declare ultrasonic-check)
(def ultrasonic-pins {1 {:echo RaspiPin/GPIO_22 :trigger RaspPin/GPIO_21}})

(def motors (atom nil))
(def arrows (atom nil))
(def servos (atom nil))
(def ultrasonic (atom nil))
(def motor-test (atom false))


(defn gpio-shutdown "Deallocate GPIO resources."
  [] (.shutdown gpio))


;;; Arrow


(defn arrow-init "Initialize an arrow with an id of 1-4."
  [id]
  (when-not (get arrows id)
    (swap! arrows assoc id (.provisionDigitalOutputPin gpio (get arrow-pins id) (str "Arrow" id) PinState/LOW))
    (println "arrow 1" (get @arrows 1))))

(defn arrow-on "Lights up an arrow by id."
  [id] (.high (get @arrows id)))

(defn arrow-off "Turns off an arrow by id."
  [id] (.low (get @arrows id)))


;;; Motor


(defn motor-init "Initialize a motor with an id of 1-4."
  [id]
  (when-not (get @motors id)
    (make-arrow id)
    (.setPwmRange @pwm 100)
    (.setPwmClock gpio 50)
    (swap! motors assoc id {:enable  (.provisionDigitalOutputPin gpio (get-in motor-pins [id :enable])
                                                                 (str "MotorEnable" id) PinState/HIGH)
                            :forward (.provisionDigitalOutputPin gpio (get-in motor-pins [id :forward])
                                                                 (str "MotorForward" id) PinState/LOW)
                            :reverse (.provisionDigitalOutputPin Gpio (get-in motor-pins [id :reverse])
                                                                 (str "MotorReverse" id) PinState/LOW)})))

(defn motor-forward "Start the motor turning in its configured \"forward\" direction."
  [id speed]
  (println "Forward")
  (if @motor-test
    (arrow-on id)
    (do
      (.setPwm @pwm speed)
      (.high (get @motors [id :forward]))
      (.low (get @motors [id :reverse))))))

(defn motor-reverse "Start the motor turning in its configured \"reverse\" direction."
  [id speed]
  (println "Reverse")
  (if @motor-test
    (arrow-off id)
    (do
      (.setPwm @pwm speed)
      (.low (get @motors [id :forward]))
      (.high (get @motors [id :reverse))))))
      

(defn motor-stop "Stop power to the motor."
  [id]
  (println "Stop")
  (arrow-off id)
  (.setPwm @pwm 0)
  (.low (get @motors [id :forward]))
  (.low (get @motors [id :reverse))))
  
(defn motor-speed "Control speed of a motor."
  [id speed]
  (.setPwm @pwm speed))


;;; Servo


(defn servo-init [id]
  (let [provider (RPIServoBlasterProvider.)]
    (swap! servos assoc id (.getServoDriver provider (get servo-pins id)))))


(defn servo-rotate
  "Rotate the servo by the range. Example: (servo-rotate 1 10 #(range 90 30 -1))"
  [id wait range-fn]
  (doseq [i (range-fn)]
    (.setServoPulseWidth (get @servos id) i)
    (Thread/sleep wait)))


;;; Ultrasonic


(defn ultrasonic-init "Initialize a sensor by id."
  [id boundary]
  (reset! ultrasonic {:boundary boundary :last-read 0})
  (println "trigger")
  (.provisionDigitalOutputPin gpio (get-in ultrasonic-pins [1 :echo]} (str "Ultrasonic" id) PinState/LOW)))

(defn ultrasonic-check [id]
  )

(defn ultrasonic-trigger [id]
  )
