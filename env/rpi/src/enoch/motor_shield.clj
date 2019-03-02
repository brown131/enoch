(ns enoch.motor-shield
  "API for Maker-Sphere Motor-Shield v1 to be used on the Raspberry Pi."
  (:import [com.pi4j.wiringpi Gpio SoftPwm]
           [com.pi4j.io.gpio GpioFactory RaspiPin PinState Pin]
           [com.pi4j.component.servo.impl RPIServoBlasterProvider]))

(def gpio (GpioFactory/getInstance))

(def motor-pins {1 {:enable RaspiPin/GPIO_00 :forward RaspiPin/GPIO_03 :reverse RaspiPin/GPIO_02}
  	         2 {:enable RaspiPin/GPIO_06 :forward RaspiPin/GPIO_04 :reverse RaspiPin/GPIO_05}
                 3 {:enable RaspiPin/GPIO_12 :forward RaspiPin/GPIO_13 :reverse RaspiPin/GPIO_14}
		 4 {:enable RaspiPin/GPIO_26 :forward RaspiPin/GPIO_10 :reverse RaspiPin/GPIO_11}})

(def arrow-pins {1 RaspiPin/GPIO_23
                 2 RaspiPin/GPIO_24
                 3 RaspiPin/GPIO_25
                 4 RaspiPin/GPIO_27})

(def servo-pins {1 RaspiPin/GPIO_07
                 2 RaspiPin/GPIO_01})

(def ultrasonic-pins {1 {:echo RaspiPin/GPIO_22 :trigger RaspiPin/GPIO_21}})

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
    (println "arrow" id (get @arrows id))))

(defn arrow-on "Lights up an arrow by id."
  [id]
  (arrow-init id)
  (.high (get @arrows id)))

(defn arrow-off "Turns off an arrow by id."
  [id]
  (arrow-init id)
  (.low (get @arrows id)))


;;; Motor


(defn motor-init "Initialize a motor with an id of 1-4."
  [id]
  (when-not (get @motors id)
    (swap! motors assoc id {:enable (.provisionDigitalOutputPin gpio (get-in motor-pins [id :enable]) (str "MotorEnable" id))
                            :forward (.provisionDigitalOutputPin gpio (get-in motor-pins [id :forward]) (str "MotorForward" id))
                            :reverse (.provisionDigitalOutputPin gpio (get-in motor-pins [id :reverse]) (str "MotorReverse" id))}))    (SoftPwm/softPwmCreate (.getAddress (get-in motor-pins [id :forward])) 0 100)
    (SoftPwm/softPwmCreate (.getAddress (get-in motor-pins [id :reverse])) 0 100))

(defn motor-forward "Start the motor turning in its configured \"forward\" direction."
  [id speed]
  (println "motor forward" id speed)
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
  (println "motor reverse" id speed)
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
  (println "motor stop" id)
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


(defn servo-init [id]
  (when-not (get @servos id)
    (swap! servos assoc id (.provisionSoftPwmOutputPin gpio (get servo-pins id)))
    (.setPwmRange (get @servos id) 900)))
  
(defn servo-rotate
  "Rotate the servo by the range. Range is from 0 to 180 (degrees).
   Example: (servo-rotate 1 10 #(range 90 30 -1))"
  [id wait range-fn]
  (servo-init id)
  (doseq [dist (range-fn)]
    (.setPwm (get @servos id) dist)
    (Thread/sleep wait)))


;;; Ultrasonic


(defn ultrasonic-init "Initialize a sensor by id."
  [id boundary]
  (reset! ultrasonic {:boundary boundary :last-read 0})
  (println "trigger")
  (.provisionDigitalOutputPin gpio (get-in ultrasonic-pins [1 :echo]) (str "Ultrasonic" id) PinState/LOW))

(defn ultrasonic-check [id]
  )

(defn ultrasonic-trigger [id]
  )