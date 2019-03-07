# enoch

This is a Clojure app designed to run my 'bot, Enoch. My robot is a Raspberry Pi controlled car with various added 
components. It will eventually be capable of understanding vocal commands and some object recognition, using Azure Speech 
Recognition and Face Recognition services.

While this software is specific to this robot, you may find the [motor-shield](/env/rpi/src/enoch/motor_shield.clj) 
namespace interesting as an example of how to implement motors, servos, and an ultrasonic sensor in Clojure using pi4j 
and sensorblast.

I am using Clojure because of it's capacity to do Communicating Sequential Processes, which allows many different 
actions to occur concurrently. And also because Clojure is cool!

I started out using Python because many of the component controllers were pre-existing, e.g. a motor shield controller, 
but I ran into a deadend because of Python's limited concurrency capabilities, particularly with the asyncio package.

![enoch](/doc/enoch.png?raw=true)

## Parts
* Rapsberry Pi 3 B
* SB Components Motor-shield that has:
  * 4 motor controllers
  * 4 LED arrows
  * Ultrasonic sensor
  * 2 SG90 servos which control a vertical/horizontal arm mounted with a ultrasonic sensor and camera
* Camera
* Speaker
* Microphone
* Battery shield
* Wheels and chassis

## License

Copyright © 2019 Scott Brown

Distributed under the Eclipse Public License either version 1.0.
