# MiniSegway - An approach to a remotely controllable inverted pendulum

![alt remote control](https://github.com/JoschuaGosda/RobotControl/blob/master/IMGL9399.jpg)

## Getting Started

This projects implements a PID control for an inverted pendulum to a miniature segway. The control is realised using an Arduino 101 and further additional components (stated below). The miniature segway can be remotely controlled via Bluetooth Low Engergy using an Android phone's touchscreen interface. 

Click [here](https://youtu.be/_MlyZ8UsYA4) to see the robot in action.

Used components:
* Arduino/Genuino 101
* 30:1 Metal Gearmotor 37Dx68L mm with 64 CPR Encoder
* Pololu Aluminum Scooter Wheel Adapter for 6mm Shaft
* Pololu Dual VNH5019 Motor Driver Shield for Arduino
* Pololu Stamped Aluminum L-Bracket Pair for 37D mm Metal Gearmotors
* LCD 1602 display module
* Lipo battery - 3 cells, 3000mAh, 20C
* Basic electronic stuff such as resistors, capacitors, wires, ...


### Arduino 101

The arduino is programmed in *Arduino IDE* and runs the [MiniSegway_PID.ino](https://github.com/JoschuaGosda/RobotControl/blob/master/MiniSegway_PID.ino) file. The tilting angle of the segway is estimated using a Kalman filter that fuses acceleration and gyroscope sensor data. The control consists of an inner- and an outer-loop for the segway's angle and position.

To send steer commands to the segway, the Arduino acts simultaneously as a bluetooth peripheral device hosting four characteristic to read, write and notify from (depending on the configuartion). Two characteritics are used to transmit the steering data for THROTTLE and TURN while the remaining two characteristics operate as a telemtry service.
    
* *speedCharacteristic*     - Characteristic for holding the value for the desired speed. After proccessing it is integrated and used as an position setpoint going into the position control loop.
    
* *turnCharacteristic*      - Characteristic for turning. After conversion from chars to ints, it is bypassed the control loop and                                     directly added/subtracted to the dutycyle of the motors.

* *tachAngleCharacteristic* - Characteristic to monitor the current angle of the segway at the smartphone.
                                  
* *tachSpeedCharacteristic* - Characteristic to monitor the current speed of the segway at the smartphone.

Each characteristic is an 8-bit char, so it can hold values from 0 to 255. Depending on it's use it is remapped and converted to floats or ints.


### Android Smartphone

For opening the bluetooth connection an android service is running in the background and handles the [GATT](https://www.bluetooth.com/specifications/gatt/generic-attributes-overview) calls. See the solution of [Cypress Semiconductor Corporation](https://github.com/cypresssemiconductorco) that worked out very well and is the backbone of the bluetooth communication in this app.

The control interface consists of two SeekBars that are used to change the THROTTLE and TURN setpoints.
After a successful connect to the Arduino, the desired values are written in the corresponding BLE Characteristic. 

![alt remote control](https://github.com/JoschuaGosda/RobotControl/blob/master/Remote_Control.jpg)



## Author

Joschua Gosda

## License

This project is licensed under the GNU GENERAL PUBLIC LICENSE - see the [LICENSE.TXT](https://github.com/JoschuaGosda/RobotControl/blob/master/LICENSE) file for details

## Acknowledgments

Thanks to...

* [Cypress Semiconductor Corporation](https://github.com/cypresssemiconductorco) for an example of an android bluetooth service

* [TKJ Electronics](https://github.com/TKJElectronics) for providing a proper working Kalman Filter


## References

* [University of Michigan](http://ctms.engin.umich.edu/CTMS/index.php?example=InvertedPendulum&section=SystemModeling)

* [Larry McGovern](https://github.com/elkayem)

* [Balanduino](http://www.balanduino.net/)

* [Joop Brokking](https://www.youtube.com/watch?v=6WWqo-Yr8lA&index=8&list=PL8mxJLcRPjiGwBP5mI8rz6H5yGMlk0XLK&t=19s)

* [Pololu](https://www.pololu.com/)





