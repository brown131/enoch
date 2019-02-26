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

(def motors (atom nil))
(def arrows (atom nil))
(def pwm (atom nil))
(def pwm-pin RaspiPin/GPIO_01)

(defn gpio-shutdown "Deallocate GPIO resources."
  [] (.shutdown gpio))

;;; Arrow


(defn make-arrow "Create an arrow with an id of 1-4."
  [id]
  (when-not (get arrows id)
    (swap! arrows assoc id (.provisionDigitalOutputPin gpio (get arrow-pins id) (str "Arrow" id) PinState/LOW))
    (println "arrow 1" (get @arrows 1))))

(defn arrow-on "Lights up an arrow by id."
  [id] (.high (get @arrows id)))

(defn arrow-off "Turns off an arrow by id."
  [id] (.low (get @arrows id)))


;;; Motor


(defn make-motor "Create a motor with an id of 1-4."
  [id]
  (when-not (get @motors id)
    (make-arrow id)
    (reset! pwm (.provisionPwmOutputPin gpio pwm-pin))
    ;;(Gpio/pwmSetMode Gpio/PWM_MODE_MS)
    ;;(Gpio/pwmSetRange 1000)
    (.setPwmClock gpio 50)
    (swap! motors id {:enable  (.provisionDigitalOutputPin gpio (get-in motor-pins [id :enable])
                                                           (str "MotorEnable" id) PinState/HIGH)
                      :forward (.provisionDigitalOutputPin gpio (get-in motor-pins [id :forward])
                                                           (str "MotorForward" id) PinState/LOW)
		      :reverse (.provisionDigitalOutputPin Gpio (get-in motor-pins [id :reverse])
                                                           (str "MotorReverse" id) PinState/LOW)})))
