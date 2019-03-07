# enoch

A Clojure app designed to run my 'bot, Enoch. My robot is a Raspberry Pi controlled car with various added components.
The will eventually be capable of understanding vocal commands and some object recognition, using Azure Speech 
Recognition and Face Recognition services.

While this software is specific to this robot, you may find the [motor-shield.clj](https:///env/mac/src/enoch/motor_shield.clj) 
namespace interesting as an example of how to implement motors, servos, and an ultrasonic sensor in Clojure using pi4j 
and sensorblast.

I am using Clojure because of it's capacity to do Communicating Sequential Processes, which allows many different 
actions to occur concurrently. And also because Clojure is cool! 

I started out using Python because many of the component controller were pre-existing, e.g. a motor shield controller, 
but I ran into a dead-end because of Python's limitation regarding concurrency, particularly with the asyncio package.

![enoch](/doc/enoch.png?raw=true)

## Parts
* Rapsberry Pi 3 B
* SB Components Motor-shield with runs:
  * 4 motors
  * 4 LED arrows
  * Ultrasonic sensor
  * 2 SG90 servos which control vertical/horizontal arm mounted with the ultrasonic sensor and camera
* Camera
* Speaker
* Microphone
* Battery shield
* Wheels and chassis

## License

Copyright © 2019 Scott Brown

Distributed under the Eclipse Public License either version 1.0.
