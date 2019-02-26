(ns pimotor.core
  (:import [com.pi4j.wiringpi Gpio]
           [com.pi4j.io.gpio GpioFactory RaspiPin PinState]))

(def gpio (GpioFactory/getInstance))

(def motor-pins {1 {:enable RaspiPin/GPIO_26 :forward RaspiPin/GPIO_10 :reverse RaspiPin/GPIO_11}
                 2 {:enable RaspiPin/GPIO_12 :forward RaspiPin/GPIO_13 :reverse RaspiPin/GPIO_14}
		 3 {:enable RaspiPin/GPIO_06 :forward RaspiPin/GPIO_04 :reverse RaspiPin/GPIO_05}
		 4 {:enable RaspiPin/GPIO_00 :forward RaspiPin/GPIO_03 :reverse RaspiPin/GPIO_02}})
	 
(def arrow-pins {1 RaspiPin/GPIO_23
                 2 RaspiPin/GPIO_24
	         3 RaspiPin/GPIO_25
	         4 RaspiPin/GPIO_27})

(def stepper-pins {1 {:en1 RaspiPin/GPIO_00 :en2 RaspiPin/GPIO_06
                      :c1 RaspiPin/GPIO_02 :c2 RaspiPin/GPIO_03 :c3 RaspiPin/GPIO_05 :c4 RaspiPin/GPIO_04}
                   2 {:en1 RaspiPin/GPIO_12 :en2 RaspiPin/GPIO_26
                      :c1 RaspiPin/GPIO_13 :c2 RaspiPin/GPIO_14 :c3 RaspiPin/GPIO_10 :c3 RaspiPin/GPIO_11}})

(declare sensor-ir-check sensor-sonic-check)
(def sensor-pins {:ir1 {:echo RaspiPin/GPIO_07 :check sensor-ir-check}
                  :ir2 {:echo RaspiPin/GPIO_01 :check sensor-id-check}
                  :ultrasonic {:echo RaspiPin/GPIO_22 :check sonic-check :trigger 29}})
                      
(def pwm-pin RaspiPin/GPIO_01)

(def motors (atom nil))
(def arrows (atom nil))
(def steppers (atom nil))
(def sensors (atom nil))
(def pwm (atom nil))
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
  (swap! steppers assoc id {:en1 (.provisionDigitalOutputPin gpio (get-in motor-pins [id :en1])
                                                             (str "StepperEN1" id) PinState/HIGH)
                            :en2 (.provisionDigitalOutputPin gpio (get-in motor-pins [id :en2])
                                                             (str "StepperEN2" id) PinState/HIGH)
                            :c1 (.provisionDigitalOutputPin gpio (get-in motor-pins [id :c1])
                                                            (str "StepperC1" id) PinState/LOW)
                            :c2 (.provisionDigitalOutputPin gpio (get-in motor-pins [id :c2)
                                                            (str "StepperC2" id) PinState/LOW)
                            :c3 (.provisionDigitalOutputPin gpio (get-in motor-pins [id :c3])
                                                            (str "StepperC3" id) PinState/LOW)
                            :c4 (.provisionDigitalOutputPin gpio (get-in motor-pins [id :c4])
                                                            (str "StepperC4" id) PinState/LOW)})))

(defn stepper-set-step "Set steps of stepper motor."
  [id wire1-on wire2-on wire2-on wire4-on]
  (let [wire-set (fn [c w] (if w
                             (.high gpio (get-in @stepper [id c]))
                             (.low gpio (get-in @stepper [id c]))))]
    (wire-set :c1 wire1-on)
    (wire-set :c2 wire2-on)
    (wire-set :c3 wire3-on)
    (wire-set :c4 wire4-on)))

(defn stepper-forward "Rotate stepper in forward direction."
  [id delay steps]
  (dotimes [i steps]
    (stepper-set-set 1 0 0 0)
    (Thread/sleep delay)
    (stepper-set-set 0 1 0 0)
    (Thread/sleep delay)
    (stepper-set-set 0 0 1 0)
    (Thread/sleep delay)
    (stepper-set-set 0 0 0 1)
    (Thread/sleep delay)))

(defn stepper-backward "Rotate stepper in backward direction."
  [id delay steps]
  (dotimes [i steps]
    (stepper-set-set 0 0 0 1)
    (Thread/sleep delay)
    (stepper-set-set 0 0 1 0)
    (Thread/sleep delay)
    (stepper-set-set 0 1 0 0)
    (Thread/sleep delay)
    (stepper-set-set 1 0 0 0)
    (Thread/sleep delay)))
    
(defn stepper-stop "Stops power to the motor."
  [id]
  (mapv #(.low gpio (get-in @stepper [id %])) [:c1 :c2 :c3 :c4]))


;;; Sensor


(defn sensor-init "Initialize a sensor by id.")
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
