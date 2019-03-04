# enoch

A Clojure app designed to run my 'bot Enoch. My 'bot is a Raspberry Pi with a SB Components Motor-shield.

While this software is specific to this robot, you may find the motor-shield.clj namespace interesting as 
an example of how to implement motors, servos, and an ultrasonic sensor in Clojure using pi4j and 
sensorblast.

[enoch](/doc/enoch.png?raw=true)

## Parts
* Rapsberry Pi 3 B
* SB Components Motor-shield with runs:
  * Ultrasonic sensor
  * 2 SG90 servos which control vertical/horizontal arm mounted with the ultrasonic sensor and a camera
* Camera
* Speaker
* Microphone
* Battery shield
* Wheels and chassis

## License

Copyright © 2019 Scott Brown

Distributed under the Eclipse Public License either version 1.0.
