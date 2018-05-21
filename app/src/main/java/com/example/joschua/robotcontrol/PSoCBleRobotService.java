/*
Copyright (c) 2016, Cypress Semiconductor Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


For more information on Cypress BLE products visit:
http://www.cypress.com/products/bluetooth-low-energy-ble
 */

package com.example.joschua.robotcontrol;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Service for managing connection and data communication with the BLE car
 */
public class PSoCBleRobotService extends Service {

    private final static String TAG = PSoCBleRobotService.class.getSimpleName();

    public enum Velocity { THROTTLE, TURN }

    private static BluetoothManager mBluetoothManager;
    private static BluetoothAdapter mBluetoothAdapter;
    private static String mBluetoothDeviceAddress;
    private static BluetoothGatt mBluetoothGatt;

    //  Queue for BLE events
    //  This is needed so that rapid BLE events don't get dropped
    private static final Queue<Object> BleQueue = new LinkedList<>();

    // UUID for the custom motor characteristics
    private static final String baseUUID =           "00000000-0000-1000-8000-00805f9b34f";
    private static final String motorServiceUUID =   baseUUID + "0";
    private static final String TurnCharUUID =  baseUUID + "1";
    private static final String ThrottleCharUUID = baseUUID + "2";
    private static final String tachAngleCharUUID =   baseUUID + "3";
    private static final String tachSpeedCharUUID =  baseUUID + "4";
    private static final String CCCD_UUID =          "00002902-0000-1000-8000-00805f9b34fb";

    // BluetoothActivity Characteristics that we need to read/write
    private static BluetoothGattCharacteristic mThrottleCharacteristic;
    private static BluetoothGattCharacteristic mTurnCharacteristic;
    private static BluetoothGattCharacteristic mTachAngleCharacteristic;
    private static BluetoothGattCharacteristic mTachSpeedCharacteristic;

    // State (on/off), speed of the motors, and tach values
    private static boolean RobotState;
    private static int throttleValue;
    private static int turnValue;
    private static int rawAngleTach;
    private static float angleTach;
    private static int rawSpeedTach;
    private static float speedTach;

    // Actions used during broadcasts to the activity
    public static final String ACTION_CONNECTED =
            "com.cypress.academy.ble101_robot.ACTION_GATT_CONNECTED";
    public static final String ACTION_DISCONNECTED =
            "com.cypress.academy.ble101_robot.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_DATA_AVAILABLE =
            "com.cypress.academy.ble101_robot.ACTION_DATA_AVAILABLE";

    /**
     * This is a binder for the BluetoothLeService
     */
    public class LocalBinder extends Binder {
        PSoCBleRobotService getService() {
            return PSoCBleRobotService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Disconnect from the GATT database and close the connection
        disconnect();
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Implements callback methods for GATT events.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**
         * This is called on a connection state change (either connection or disconnection)
         * @param gatt The GATT database object
         * @param status Status of the event
         * @param newState New state (connected or disconnected)
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(ACTION_DISCONNECTED);
            }
        }

        /**
         * This is called when service discovery has completed.
         * It broadcasts an update to the main activity.
         * @param gatt The GATT database object
         * @param status Status of whether the discovery was successful.
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                // Get the characteristics for the motor service
                BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(motorServiceUUID));
                if (gattService == null) return; // return if the motor service is not supported
                mThrottleCharacteristic = gattService.getCharacteristic(UUID.fromString(ThrottleCharUUID));
                mTurnCharacteristic = gattService.getCharacteristic(UUID.fromString(TurnCharUUID));
                mTachAngleCharacteristic = gattService.getCharacteristic(UUID.fromString(tachAngleCharUUID));
                mTachSpeedCharacteristic = gattService.getCharacteristic(UUID.fromString(tachSpeedCharUUID));

                // Set the CCCD to notify us for the two tach readings
                setCharacteristicNotification(mTachAngleCharacteristic, true);
                setCharacteristicNotification(mTachSpeedCharacteristic, true);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * This handles the BLE Queue. If the queue is not empty, it starts the next event.
         */
        private void handleBleQueue() {
            if(BleQueue.size() > 0) {
                // Determine which type of event is next and fire it off
                if (BleQueue.element() instanceof BluetoothGattDescriptor) {
                    mBluetoothGatt.writeDescriptor((BluetoothGattDescriptor) BleQueue.element());
                } else if (BleQueue.element() instanceof BluetoothGattCharacteristic) {
                    mBluetoothGatt.writeCharacteristic((BluetoothGattCharacteristic) BleQueue.element());
                }
            }
        }

        /**
         * This is called when a characteristic write has completed. Is uses a queue to determine if
         * additional BLE actions are still pending and launches the next one if there are.
         * @param gatt The GATT database object
         * @param characteristic The characteristic that was written.
         * @param status Status of whether the write was successful.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            // Pop the item that was written from the queue
            BleQueue.remove();
            // See if there are more items in the BLE queues
            handleBleQueue();
        }

        /**
         * This is called when a CCCD write has completed. It uses a queue to determine if
         * additional BLE actions are still pending and launches the next one if there are.
         *
         * @param gatt The GATT database object
         * @param descriptor The CCCD that was written.
         * @param status Status of whether the write was successful.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            // Pop the item that was written from the queue
            BleQueue.remove();
            // See if there are more items in the BLE queues
            handleBleQueue();
        }

        /**
         * This is called when a characteristic with notify set changes.
         * It broadcasts an update to the main activity with the changed data.
         *
         * @param gatt The GATT database object
         * @param characteristic The characteristic that was changed
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // Get the UUID of the characteristic that changed
            String uuid = characteristic.getUuid().toString();

            // Update the appropriate variable with the new value.
            switch (uuid) {
                case tachAngleCharUUID:
                    rawAngleTach = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0);
                    //Transformation corresponding to Arduino data processing
                    angleTach = (float) (((float)rawAngleTach*61)/255) - 30;
                    break;
                case tachSpeedCharUUID:
                    rawSpeedTach = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0);
                    //Transformation corresponding to Arduino data processing
                    speedTach = (float) ((float) (((double)rawSpeedTach*7)/255) - 2.96);
                    break;
            }
            // Tell the activity that new car data is available
            broadcastUpdate(ACTION_DATA_AVAILABLE);
        }
    };


    /**
     * Sends a broadcast to the listener in the main activity.
     * @param action The type of action that occurred.
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }


    /**
     * Initialize a reference to the local BluetoothActivity adapter.
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        // Initialize robot state variables
        RobotState = false;
        throttleValue = 127;
        turnValue = 127;

        return true;
    }

    /**
     * Connects to the GATT server hosted on the BluetoothActivity LE device.
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.i(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;



        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Update the speed of the GATT database. the speed
     * value comes from the global variables turnValue or throttleValue which are
     * set by the setVelocity function.
     * @param state determines if motor is on or off
     */
    private void updateGattVelocity( boolean state)
    {
            if (mThrottleCharacteristic != null) {
                if(state) {
                    mThrottleCharacteristic.setValue(throttleValue, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
                } else {
                    mThrottleCharacteristic.setValue(127, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
                }
                writeCharacteristic(mThrottleCharacteristic);
            }
            if (mTurnCharacteristic != null) {
                if(state) {
                    mTurnCharacteristic.setValue(turnValue, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
                } else {
                    mTurnCharacteristic.setValue(127, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
                }
                writeCharacteristic(mTurnCharacteristic);
            }
            //Log.d(TAG, "Werte gesetzt! THROTTLE:" + throttleValue + "      TURN:" + turnValue);
        }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic The characteristic to write.
     */
    private void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BleQueue.add(characteristic);
        if (BleQueue.size() == 1) {
            mBluetoothGatt.writeCharacteristic(characteristic);
            Log.i(TAG, "Writing Characteristic");
        }
    }

    /**
     * Enables or disables notification on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                               boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.i(TAG, "BluetoothAdapter not initialized");
            return;
        }

        /* Enable or disable the callback notification on the phone */
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        /* Set CCCD value locally and then write to the device to register for notifications */
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(CCCD_UUID));
        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        // Put the descriptor into the write queue
        BleQueue.add(descriptor);
        // If there is only 1 item in the queue, then write it. If more than one, then the callback
        // will handle it
        if (BleQueue.size() == 1) {
            mBluetoothGatt.writeDescriptor(descriptor);
            Log.i(TAG, "Writing Notification");
        }
    }

    /**
     * Turn a motor on/off
     * @param state turn motor on or off
     */
    public void setRobotState( boolean state) {
            RobotState = state;
        updateGattVelocity(state);
    }


    /**
     * Set the speed setting of one of the motors.
     * Note that this is only the requested speed. It will not
     * be written into the GATT database unless the switch is
     * turned on.
     *
     * @param velocity to operate on
     * @param value to set the motor to
     */
    public void setVelocity(Velocity velocity, int value) {
        boolean state;
        if(velocity == Velocity.THROTTLE)
        {
            throttleValue = value;
            state = RobotState;
        } else  {
            turnValue = value;
            state = RobotState;
        }
        // Update the Speed in the Gatt Database
        updateGattVelocity(state);
    }

    /**
     * Get the tach reading for one of the motors
     * @param velocity to operate on
     * @return tach value
     */
    public static float getTach(Velocity velocity) {
        if (velocity == Velocity.THROTTLE) {
            return angleTach;
        } else {
            return speedTach;
        }
    }

    /**
     * This function returns the UUID of the motor service
     * @return the motor service UUID
     */
    public static UUID getMotorServiceUUID() {
        return UUID.fromString(PSoCBleRobotService.motorServiceUUID);
    }
}
