# MiniSegway

This is the approach of a remote controlled MiniSegway that stabilises itself from falling over.

![alt remote control](https://github.com/JoschuaGosda/RobotControl/blob/master/IMGL9399.jpg)

## Getting Started

This projects consists of an Arduino 101 and an smartphone running Android. While the Arduino is responsible for the feedback control and acting as a bluetooth peripheral at the same time, the Android Phone  allows the user to interact with the robot. 

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

The arduino is programmed in *Arduino IDE* and runs the [MiniSegway_PID.ino](https://github.com/JoschuaGosda/RobotControl/blob/master/MiniSegway_PID.ino) file. Onboard are acceleration- and gyrosensors as well as a built-in BluetoothLowEnergy module. 
The sensor data is used to determine the angle of the segway using a Kalman Filter. It is then given to the feedback control to keep the segway upright by adjusting motor torque.
To make sure that the segway isn't moving with constant speed while the angle remains zero another feedback control is used to locate the position.

The controller scematic looks like followed:
![alt controller scmatic](https://github.com/JoschuaGosda/RobotControl/blob/master/contoller_scematic.PNG)

It consists of an inner- and an outer-loop as a result that the segway is a SIMO-system (Single Input - Mulitiple Outputs) and [classical control](https://en.wikipedia.org/wiki/Classical_control_theory) is used.
Dependending on the difference between  desired- and actual values, the PID-controllers calculate an appropriate output.
Now, the idea behind all this is to remotely control the segway. Therefore the desired value for the outer-loop has to  be changed depending on user input. Otherwise the robot  would just stay at the same spot.

 For this task the arduino simultaneously acts as a bluetooth peripheral device which provides an bluetooth service hosting four characteristic to read, write and notify from (depending on the configuartion).
In this project two characteritics are used to transmit the steering data for THROTTLE and TURN while the remaining two characteristics operate as a telemtry service.
    
* *speedCharacteristic*     - Characteristic for holding the value for the desired speed. After proccessing it is integrated and used as an position setpoint going into the position control loop.
    
* *turnCharacteristic*      - Characteristic for turning. After conversion from chars to ints, it is bypassed the control loop and                                     directly added/subtracted to the dutycyle of the motors.

* *tachAngleCharacteristic* - Characteristic to monitor the current angle of the segway at the smartphone.
                                  
* *tachSpeedCharacteristic* - Characteristic to monitor the current speed of the segway at the smartphone.

Each characteristic is an 8-bit char, so it can hold values from 0 to 255. Depending on it's use it is remapped and converted to floats or ints.


### Android Smartphone

The app running on the phone is developed in *Android Studio*. It connects to the arduino board over a bluetooth connection to share steer commands as well as to show telemtry data. 
For opening the bluetooth connection an android service is running in the background and handles the [GATT](https://www.bluetooth.com/specifications/gatt/generic-attributes-overview) calls. Please see the solution of [Cypress Semiconductor Corporation](https://github.com/cypresssemiconductorco) that worked out very well and is the backbone of the bluetooth communication in this app.

The controlling surface in the app is very basic. Two SeekBars are used to change the THROTTLE and TURN setpoints.
After a successfully connect to the arduino board, the desired values are written in the corresponding BLE Characteristic. 

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





