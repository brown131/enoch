(ns pimotor.core
  (:import [com.pi4j.wiringpi Gpio]
           [com.pi4j.io.gpio GpioFactory RaspiPin PinState]
           [com.pi4j.component.motor.impl GpioStepperMotorComponent]))

(def gpio (GpioFactory/getInstance))

(def motor-pins {1 {:enable RaspiPin/GPIO_26 :forward RaspiPin/GPIO_10 :reverse RaspiPin/GPIO_11}
                 2 {:enable RaspiPin/GPIO_12 :forward RaspiPin/GPIO_13 :reverse RaspiPin/GPIO_14}
		 3 {:enable RaspiPin/GPIO_06 :forward RaspiPin/GPIO_04 :reverse RaspiPin/GPIO_05}
		 4 {:enable RaspiPin/GPIO_00 :forward RaspiPin/GPIO_03 :reverse RaspiPin/GPIO_02}})
	 
(def arrow-pins {1 RaspiPin/GPIO_23
                 2 RaspiPin/GPIO_24
	         3 RaspiPin/GPIO_25
	         4 RaspiPin/GPIO_27})

(def stepper-pins {1 {:controller1 RaspiPin/GPIO_02
                      :controller2 RaspiPin/GPIO_03
                      :controller3 RaspiPin/GPIO_05
                      :controller4 RaspiPin/GPIO_04}
                   2 {:controller1 RaspiPin/GPIO_13
                      :controller2 RaspiPin/GPIO_14
                      :controller3 RaspiPin/GPIO_10
                      :controller4 RaspiPin/GPIO_11}})

(declare sensor-ir-check sensor-sonic-check)
(def sensor-pins {:ir1 {:echo RaspiPin/GPIO_07 :check sensor-ir-check}
                  :ir2 {:echo RaspiPin/GPIO_01 :check sensor-ir-check}
                  :ultrasonic {:echo RaspiPin/GPIO_22 :check sensor-sonic-check :trigger 29}})
                      
(def pwm-pin RaspiPin/GPIO_01)

(def motors (atom nil))
(def arrows (atom nil))
(def steppers (atom nil))
(def stepper-controllers (atom nil))
(def sensors (atom nil))
(def pwm (atom nil))
(def motor-test (atom false))

(defn gpio-shutdown "Deallocate GPIO resources."
  [] (.shutdown gpio))


;;; Arrow


(defn arrow-init "Initialize an arrow with an id of 1-4."
  [id]
  (when-not (get @arrows id)
    (swap! arrows assoc id (.provisionDigitalOutputPin gpio (get arrow-pins id) (str "Arrow" id) PinState/LOW))
    (println "arrow" id (get @arrows 1))))

(defn arrow-on "Lights up an arrow by id."
  [id] (.high (get @arrows id)))

(defn arrow-off "Turns off an arrow by id."
  [id] (.low (get @arrows id)))


;;; Motor


(defn motor-init "Initialize a motor with an id of 1-4."
  [id]
  (when-not (get @motors id)
    (arrow-init id)
    (reset! pwm (.provisionPwmOutputPin gpio pwm-pin))
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
      (.low (get @motors [id :reverse])))))

(defn motor-reverse "Start the motor turning in its configured \"reverse\" direction."
  [id speed]
  (println "Reverse")
  (if @motor-test
    (arrow-off id)
    (do
      (.setPwm @pwm speed)
      (.low (get @motors [id :forward]))
      (.high (get @motors [id :reverse])))))
      

(defn motor-stop "Stop power to the motor."
  [id]
  (println "Stop")
  (arrow-off id)
  (.setPwm @pwm 0)
  (.low (get @motors [id :forward]))
  (.low (get @motors [id :reverse])))
  
(defn motor-speed "Control speed of a motor."
  [id speed]
  (.setPwm @pwm speed))


;;; Motors


(defn motors-forward "Start the motor turning in its configured \"forward\" direction."
  [ids]
  (doseq [id ids]
    (motor-forward id)))

(defn motors-reverse "Start the motor turning in its configured \"reverse\" direction."
  [ids]
  (doseq [id ids]
    (motor-reverse id)))

(defn motors-stop "Stop power to the motor."
  [ids]
  (doseq [id ids]
    (motor-stop id)))


;;; Stepper

(defn stepper-init "Initialize a stepper motor with an id 1-2."
  [id]
  (swap! stepper-controllers assoc id
         (into-array [(.provisionDigitalOutputPin gpio (get-in stepper-pins [id :controller1])
                                                  (str "StepperController1" id) PinState/LOW)
                      (.provisionDigitalOutputPin gpio (get-in stepper-pins [id :controller2])
                                                  (str "StepperController2" id) PinState/LOW)
                      (.provisionDigitalOutputPin gpio (get-in stepper-pins [id :controller3])
                                                  (str "StepperController3" id) PinState/LOW)
                      (.provisionDigitalOutputPin gpio (get-in stepper-pins [id :controller4])
                                                  (str "StepperController4" id) PinState/LOW)]))
  (.setShutdownOptions gpio true com.pi4j.io.gpio.PinState/LOW (get @stepper-controllers id))
  (swap! steppers assoc id (GpioStepperMotorComponent. (get @stepper-controllers id))))

(defn stepper-set-step "Set steps of stepper motor."
  [id wire1-on wire2-on wire3-on wire4-on]
  (let [wire-set (fn [c w] (if (zero? w)
                             (.low gpio (get-in @steppers [id c]))
                             (.high gpio (get-in @steppers [id c]))))]
    (wire-set :controller1 wire1-on)
    (wire-set :controller2 wire2-on)
    (wire-set :controller3 wire3-on)
    (wire-set :controller4 wire4-on)))

(defn stepper-forward "Rotate stepper in forward direction."
  [id delay steps]
  (let [stepper (get @steppers id)]
    (.setStepInterval stepper delay)
    (.setStepSequence stepper (byte-array [8 4 2 1]))
    (.setStepsPerRevolution stepper 180)
    (.step stepper steps)))

(defn stepper-backward "Rotate stepper in backward direction."
  [id delay steps]
  (let [stepper (get @steppers id)]
    (.setStepInterval stepper delay)
    (.setStepSequence stepper (byte-array [1 2 4 8]))
    (.setStepsPerRevolution stepper 180)
    (.step stepper steps)))
    
(defn stepper-stop "Stops power to the motor."
  [id]
  (mapv #(.low gpio (get-in @steppers [id %])) [:controller1 :controller2 :controller3 :controller4]))


;;; Sensor


(defn sensor-init "Initialize a sensor by id."
  [id boundary]
  (swap! sensors assoc type (assoc (get sensor-pins id) :boundary boundary :last-read 0))
  (when (= id :ultrasonic)
    (println "trigger")
    (.provisionDigitalOutputPin gpio (get sensor-pins id) (str "Sensor" id) PinState/LOW)))
  
(defn sensor-ir-check
  [id]
  )

(defn sensor-sonic-check
  [id]
  )
