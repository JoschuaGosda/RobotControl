#include <Kalman.h>
#include <CurieBLE.h>
#include <CurieIMU.h>
#include <LiquidCrystal.h>

/*PORT MANIPULATION*/
uint32_t ioReg1=SS_GPIO_8B1_BASE_ADDR+SS_GPIO_EXT_PORTA;
uint32_t ioReg2=SOC_GPIO_BASE_ADDR+SOC_GPIO_EXT_PORTA;
uint32_t ioRegA=SS_GPIO_8B0_BASE_ADDR+SS_GPIO_EXT_PORTA;

/*PINS*/
#define EncoderB_right (__builtin_arc_lr(ioReg1)&0b00100000)  //Pin 8           pin_state = EncoderB_right!=0;  0 for LOW; 1 for HIGH
#define EncoderB_left (__builtin_arc_lr(ioReg1)&0b01000000)   //Pin 9           pin_state = EncoderB_left!=0;  0 for LOW; 1 for HIGH
#define EncoderA_right 11
#define EncoderA_left 10

//M1_inA/B and M2_inA/B determines directions of motors by writing A - HIGH and B - LOW or the other way round
#define M1_inA 4
#define M1_inB 2
#define M2_inA 7
#define M2_inB 3
#define M1_PWM 5    //left motor
#define M2_PWM 6    //right motor

#define BUTTON A5
#define VOLTAGE A4

/*VARIABLE SETTINGS*/
#define CALC_FREQ 200        //  frequency of control loop in Hz
#define SAMPLERATE 1600      //  available values for CurieIMU module: 25, 50, 100, 200, 400, 800, 1600, 3200

//PID's for angle control - inner loop
float Kp[2] = {20.0f, 10.0f};
float Ki[2] = {3.0f, 4.0f};
float Kd[2] = {2.0f, 2.0};;

//PID's for position control - outer loop
float KP[2] = {0.1f,  3.0f};
float KI[2] = {0, 0};
float KD[2] = {5.0f, 2.0f};

float max_speed = 0.85f;
float max_turn = 50.0f;

/*VARIABLES FOR INTERRUPTS*/
//volatiles are used as the ticks_L & ticks_R are used in an interrupt and have to be accessed and written fast
volatile long ticks_L = 0, ticks_R = 0;

/*INITIALISE LCD LIBRARY*/
LiquidCrystal lcd(12, 13, A0, A1, A2, A3);
int lcdsize[] = {16, 2};                      //16 colums, 2 rows

/*KALMAN CONFIGURATION*/
Kalman kalmanY;

/*RAW Values*/
int aRawX, aRawY, aRawZ;
int gRawX, gRawY, gRawZ;

/* IMU Data */
double accX, accY, accZ;
double gyroX, gyroY, gyroZ;
int16_t tempRaw;

double gyroXangle, gyroYangle;                // Angle calculate using the gyro only
double compAngleX, compAngleY;                // Calculated angle using a complementary filter
double kalAngleX, kalAngleY, kalAngleY_filt;  // Calculated angle using a Kalman filter
int counter = 0;
uint32_t kal_timer;

/*BLUETOOTH CONFIGURATION*/
BLEPeripheral blePeripheral;
BLEService mService("00000000-0000-1000-8000-00805f9b34f0"); // create service

//BLEUnsignedCharCharacteristic --> send 1 unsigned char
BLEUnsignedCharCharacteristic turnCharacteristic("00000000-0000-1000-8000-00805f9b34f1", BLERead | BLEWrite);
BLEUnsignedCharCharacteristic speedCharacteristic("00000000-0000-1000-8000-00805f9b34f2", BLERead | BLEWrite);
BLEUnsignedCharCharacteristic tachAngleCharacteristic("00000000-0000-1000-8000-00805f9b34f3", BLERead | BLEWrite | BLENotify);
BLEUnsignedCharCharacteristic tachSpeedCharacteristic("00000000-0000-1000-8000-00805f9b34f4", BLERead | BLEWrite | BLENotify);

/*PROTOTYPES*/
float convertRawAcceleration(int aRaw);
float convertRawGyro(int gRaw);
void InterruptA_right();
void InterruptA_left();

void setup() {
  Serial.begin(9600);

/*START SENSOR READINGS & KALMAN*/
  // start the IMU and filter
  CurieIMU.begin();
  CurieIMU.setGyroRate(SAMPLERATE);
  CurieIMU.setAccelerometerRate(SAMPLERATE);

  // Set the accelerometer range to 2G
  CurieIMU.setAccelerometerRange(2);
  // Set the gyroscope range to 250 degrees/second
  CurieIMU.setGyroRange(250);
  CurieIMU.setGyroOffset(Y_AXIS, -0.33);
  
  CurieIMU.readMotionSensor(aRawX, aRawY, aRawZ, gRawX, gRawY, gRawZ);
  // convert from raw data to gravity and degrees/second units
  accX = convertRawAcceleration(aRawX);
  accZ = convertRawAcceleration(aRawZ);
  gyroY = convertRawGyro(gRawY);

  double pitch = atan2(-accX, accZ) * RAD_TO_DEG;
  kalmanY.setAngle(pitch);
  gyroYangle = pitch;
  compAngleY = pitch;

  kal_timer = micros();

  //declare I/O
  pinMode(BUTTON, INPUT);
  pinMode(8, INPUT_PULLUP);        //portmanipulation pin
  pinMode(9, INPUT_PULLUP);        //portmanipulation pin
  pinMode(2, OUTPUT);
  pinMode(3, OUTPUT);
  pinMode(4, OUTPUT);
  pinMode(7, OUTPUT);
  

  //set cursor position & message
  lcd.begin(lcdsize[0], lcdsize[1]);

  delay(500);

/*INITIALISE BLUETOOTH SERVICE*/
 // set the local name peripheral advertises
  blePeripheral.setLocalName("MiniSegway");
  // set the UUID for the service this peripheral advertises:
  blePeripheral.setAdvertisedServiceUuid(mService.uuid());

  // add the characteristics to the service
  blePeripheral.addAttribute(mService);
  blePeripheral.addAttribute(speedCharacteristic);
  blePeripheral.addAttribute(turnCharacteristic);
  blePeripheral.addAttribute(tachAngleCharacteristic);
  blePeripheral.addAttribute(tachSpeedCharacteristic);

  speedCharacteristic.setValue(127); //start value == 0 speed
  turnCharacteristic.setValue(127);  //start value == 0 turn
  
  // start advertising
  blePeripheral.begin();  
  delay(500);
}

void loop() {

/*INITIALISE*/
  //Flags
  static boolean control = false;
  static boolean button_pressed = false;
  static boolean standing_still = true;

  //Timer & Ticks
  static unsigned long int  stateTimer, voltageTimer, sensorTimer, bleTimer, gainTimer = 0;
  static long ticks_right, ticks_left;

  //Control
  int unsigned calc_time = (int) (1000000/CALC_FREQ); //time in micros
  static float phi, e_phi,  e_phi_int, e_phi_der, phi_pid, phi_des;
  static float xdot, e_x, e_x_old, e_x_int, e_x_der, x_pid, x_des;
  static float x, x_old, dt, u;                

  //Voltage Check
  static float voltage, voltage_old, voltage_div;
  static int i;

  //Motor Control
  static int duty_cycle_1 = 0, duty_cycle_2 = 0;

  //Ble Connection
  static unsigned char desired_speed_, desired_turn_, measured_speed, measured_angle;
  static float desired_speed, desired_turn;
  static float xdot_, phi_; 

  // poll for BLE events
  blePeripheral.poll();

  //Checks if the connection to the central is active or not
  BLECentral central = blePeripheral.central();

  //attach Interrupt to the rising edge of the encoders
  attachInterrupt(EncoderA_right, InterruptA_right, RISING);
  attachInterrupt(EncoderA_left, InterruptA_left, RISING);

/*ENCODER AND SENSOR DATA*/
  //avoid interrupts, otherwise an error can occur when data is accessed and written at the same time
  noInterrupts();
  ticks_right = ticks_R;
  ticks_left = ticks_L;
  CurieIMU.readMotionSensor(aRawX, aRawY, aRawZ, gRawX, gRawY, gRawZ);
  interrupts();

  // convert from raw data to gravity and degrees/second units
  accX = convertRawAcceleration(aRawX);
  accZ = convertRawAcceleration(aRawZ);
  gyroY = convertRawGyro(gRawY);


/*KALMAN FILTER*/
  double dt_kal = (double)(micros() - kal_timer) / 1000000; // Calculate delta time
  kal_timer = micros();

  //atan2 outputs the value of -π to π (radians) It is then converted from radians to degrees
  double pitch = atan2(-accX, accZ) * RAD_TO_DEG;

  // This fixes the transition problem when the accelerometer angle jumps between -180 and 180 degrees
  if ((pitch < -90 && kalAngleY > 90) || (pitch > 90 && kalAngleY < -90)) {
    kalmanY.setAngle(pitch);
    compAngleY = pitch;
    kalAngleY = pitch;
    gyroYangle = pitch;
  } else
    kalAngleY = kalmanY.getAngle(pitch, gyroY, dt_kal); // Calculate the angle using a Kalman filter

  // Reset the gyro angle when it has drifted too much
  if (gyroYangle < -180 || gyroYangle > 180)
    gyroYangle = kalAngleY;

    kalAngleY -= 2.0f; //offset
    kalAngleY_filt += kalAngleY;
    counter++;

/*LCD DISPLAY*/
  if(!control){
    if(!button_pressed){
      lcd.clear();
      lcd.print("Press Button");
      lcd.setCursor(0, 1);
      lcd.print("to start");
      while(!button_pressed){
          if(analogRead(BUTTON) > 900){
            button_pressed = true;
            lcd.clear();
          }
      }
    }
  else{
    lcd.setCursor(0, 0);
    lcd.print("Stand me up!");
    lcd.setCursor(0, 1);
    lcd.print(kalAngleY);
      if((kalAngleY < 0.1) && (kalAngleY > -0.1)){
          //In an almost perfect vertical position the flag for the feedback control is set
          control = true;           
        
/*INITIALISE/START CONDITIONS*/
          //Encoder ticks
          ticks_L = 0;
          ticks_R = 0;
          
          //vars for outer loop
          x = 0;
          x_old = 0;
          x_des = 0;
          xdot = 0;
          e_x = 0;
          e_x_old = 0;
          e_x_int = 0;
          e_x_der = 0;
          
          //vars for inner loop
          phi_des = 0;
          phi = 0;
          e_phi = 0;
          e_phi_int = 0;
          e_phi_der = 0;
          
          //desired parameter - transmitted trough bluetooth
          desired_speed = 0;
          desired_speed_ = 0;
          desired_turn = 0;
          desired_turn_ = 0;
          
          //average filter
          kalAngleY_filt = 0;
          counter = 0;

         // set the timer to the current time
          stateTimer = micros();
          voltageTimer = millis();
          sensorTimer = millis();
          bleTimer = millis();
          lcd.clear();
      }
    }
 }
  
/*CONTROL SEQUENCE*/
  else{     //the control flag is true
  
/*VOLTAGE CHECK*/
    if((millis() - voltageTimer) > 100){
      //for loop is used to software-filter the voltage data
      for(i = 0; i < 30; i++){
        if(i == 0){
          voltage_div = analogRead(VOLTAGE);
        }
        else{
         voltage_div += analogRead(VOLTAGE);
        }
      }
      voltage_div /= 30;
      voltage_div = voltage_div *(3.3f/1023);
      voltage = voltage_div * (40.f/10);    //Voltage divider: R_ges = 40kOhm, R_1 = 30kOhm, R_2 = 10kOhm

      //avoid the noise constantly reprinting the display
      if(abs(voltage - voltage_old) > 0.05f){
        lcd.clear();
        lcd.print("Voltage: ");
        lcd.setCursor(9, 0);
        lcd.print(voltage);
        lcd.setCursor(0, 1);
        lcd.print("Go, go, go!");
        voltage_old = voltage;
        } 
      }
      //treshhold for remote control - control gains are inappropriate if voltage drops too low
      if(voltage < 10.5f){
        lcd.clear();
        lcd.print("Please recharge");
        lcd.setCursor(0, 1);
        lcd.print("battery");
      }
  
/*PID CONTROL*/
  //more information to closed-loop feedback control: https://en.wikipedia.org/wiki/Control_theory
  if((micros() - stateTimer) > calc_time){
    dt = (double) calc_time/1000000;
    x_des += desired_speed*dt;  //integrate speed setpoint coming from bluetooth connection

    if(desired_speed != 0){
      gainTimer = millis();
    }

    x = (float) - 0.229f * 0.001 * (ticks_right+ticks_left); //[m] (1/(2*480)) * 70 *pi = 0.229 -- 480 counts/revolution
    xdot = (x - x_old)/dt;
    //deviance between desired position and actual value
    e_x = x_des - x;

    //intergration and derivation for PID calculation - outer loop
    e_x_int += e_x;
    e_x_der = (e_x - e_x_old)/dt; //[m/s]

    //gains for standing still
    if((millis() - gainTimer) > 800){ 
      if(!standing_still){
        x_des = x;
      }
      standing_still = true;
      x_pid = KP[1] * e_x + KI[1] * e_x_int + KD[1] * e_x_der;

    //constrain the output so that inner loop doesn't go unstable
    phi_des = constrain(x_pid, -8, 8);

    //kalAngleY ist filtered by an average filter
    phi = - (kalAngleY_filt/counter);
    //deviance between desired angle and actual value
    e_phi = phi_des - phi;
    //intergration and derivation for PID calculation - inner loop
    e_phi_int += e_phi;
    e_phi_der = gyroY;

    
    phi_pid = Kp[1] * e_phi + Ki[1] * e_phi_int + Kd[1] * e_phi_der;
      
    }
    //gains for driving - agressive
    else{
      standing_still = false;
      x_pid = KP[0] * e_x + KI[0] * e_x_int + KD[0] * e_x_der;
    
    //constrain the output so that inner loop doesn't go unstable
    phi_des = constrain(x_pid, -6, 6);

    //kalAngleY ist filtered by an average filter
    phi = - (kalAngleY_filt/counter);
    //deviance between desired angle and actual value
    e_phi = phi_des - phi;
    //intergration and derivation for PID calculation - inner loop
    e_phi_int += e_phi;
    e_phi_der = gyroY;

    phi_pid = Kp[0] * e_phi + Ki[0] * e_phi_int + Kd[0] * e_phi_der;
    }

    //u is after some more modification used to set the duty cycles of the motors
    u = phi_pid;

    //Overwriting/Reseting for next control cycle
    x_old = x;
    e_x_old = e_x;
    kalAngleY_filt = 0;
    counter = 0;

/*BLUETOOTH COMMUNICATION*/
    //data proccessing for telemetry going to the smartphone
    phi_ = constrain(phi, -30, 30);
    xdot_ = constrain(xdot, -3, 3);
    phi_ = (phi_ + 30) *(255/61);
    xdot_ = (xdot_ + 3)*(255/7);
    measured_angle = (char) phi_;
    measured_speed = (char) xdot_;

    //only access the bluetooth functions/interrups at intervals higher 20ms - otherwise Arduino leans to crash
    if((millis() - bleTimer) > 100){
      if(central){
        //get the values that are written in the bluetooth characteristics
        desired_speed_ = speedCharacteristic.value();
        desired_turn_ = turnCharacteristic.value();
        
        //transformation from 8 bit chars to float
        desired_speed = (int) desired_speed_;
        //be aware if the max_speed factor is too high the robot can't follow/work properly and may fall over
        desired_speed = (desired_speed - 127) * ((2*max_speed)/254);

        //transformation from 8 bit chars to int - desired_turn is directly added/subtracted to dutycycle
        desired_turn = (int) desired_turn_;
        desired_turn = (int) ((desired_turn - 127) * ((2*max_turn)/254));

        //put the proccessed data in the bluetooth characteristic
        tachSpeedCharacteristic.setValue(measured_speed);
        tachAngleCharacteristic.setValue(measured_angle);
      }
      else{
        //if the connection is lost or the voltage drops too low the robot remains at the same position
        desired_speed_ = 0;
        desired_turn_ = 0;
      }
      bleTimer = millis();
   }

/*MOTOR CONTROL*/
    if(standing_still && (u < 0)){
      duty_cycle_1 = (int) u + desired_turn; 
      duty_cycle_2 = (int) u - desired_turn + 2;
    }
    else{
      duty_cycle_1 = (int) u + desired_turn; 
      duty_cycle_2 = (int) u - desired_turn;
    }

     
    duty_cycle_1 = constrain(duty_cycle_1, -255, 255);
    duty_cycle_2 = constrain(duty_cycle_2, -255, 255);
  
    if(duty_cycle_1 >= 0){
      //forwards M1
      digitalWrite(M1_inA, HIGH);
      digitalWrite(M1_inB, LOW);
    }
    else{
      //backwards M1
      digitalWrite(M1_inA, LOW);
      digitalWrite(M1_inB, HIGH);
      duty_cycle_1 = -duty_cycle_1;
    }
    analogWrite(M1_PWM, duty_cycle_1); 
  
    if(duty_cycle_2 >= 0){
      //forwards M2
      digitalWrite(M2_inA, LOW);
      digitalWrite(M2_inB, HIGH);
    }
    else{
      //backwards M2
      digitalWrite(M2_inA, HIGH);
      digitalWrite(M2_inB, LOW);
      duty_cycle_2 = -duty_cycle_2;
    }
    analogWrite(M2_PWM, duty_cycle_2);
     
    //Robot has fallen down
    if((phi > 30) || (phi < -30)){
      //exit to feedback control loop
      control = false;
      button_pressed = false;
      //set PWM signals to 0 and make the motors stop
      analogWrite(M1_PWM, 0);  
      analogWrite(M2_PWM, 0); 
    }
    stateTimer = micros();
    }
  }
}//end of loop


/*FUNCTIONS*/
void InterruptA_right(){
  //check encoder B for determine direction -  port manipulation is used for faster digital reading
  if(EncoderB_right!=0){
    ticks_R -= 1;
  }
  else{
    ticks_R += 1;
  }
}

void InterruptA_left(){
  //check encoder B for determine direction -  port manipulation is used for faster digital reading
  if(EncoderB_left!=0){
    ticks_L += 1;
  }
  else{
    ticks_L -= 1;
  }
}

float convertRawAcceleration(int aRaw) {
  // since we are using 2G range
  // -2g maps to a raw value of -32768
  // +2g maps to a raw value of 32767
  
  float acc = (aRaw * 2.0) / 32768.0;
  return acc;
}

float convertRawGyro(int gRaw) {
  // since we are using 250 degrees/seconds range
  // -250 maps to a raw value of -32768
  // +250 maps to a raw value of 32767
  
  float gyro = (gRaw * 250.0) / 32768.0;
  return gyro;
}
