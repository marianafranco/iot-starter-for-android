package com.ibm.iot.android.iotstarter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ibm.iot.android.iotstarter.IoTStarterApplication;

import java.util.Timer;
import java.util.TimerTask;

import java.util.Random;

/**
 * This class simulates a sensor. The sensor value increases until MAX_VALUE.
 *
 * Created by Mariana Ramos Franco on 10/14/15.
 */
public class FakeSensor {
    private final String TAG = DeviceSensor.class.getName();
    private static FakeSensor instance;
    private IoTStarterApplication app;
    private Context context;
    private boolean isEnabled = false;
    private Timer timer;
    private Random rand;
    private int sensorValue;
    SharedPreferences settings;

    public static final String FAKE_SENSOR_VALUE = "FakeSensorValue";

    public static final int TIMER_PERIOD = 10000;   // 10s
    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 1000;
    public static final int MIN_INC_VALUE = 0;
    public static final int MAX_INC_VALUE = 10;

    public FakeSensor(Context context) {
        this.context = context;
        rand = new Random();
        app = (IoTStarterApplication) context.getApplicationContext();

        settings = context.getSharedPreferences(FAKE_SENSOR_VALUE, 0);
        sensorValue = settings.getInt("data", generateInitValue());
        app.setSensorValue(sensorValue);
    }

    /**
     * @param context The application context for the object.
     * @return The MqttHandler object for the application.
     */
    public static FakeSensor getInstance(Context context) {
        if (instance == null) {
            Log.i(FakeSensor.class.getName(), "Creating new DeviceSensor");
            instance = new FakeSensor(context);
        }
        return instance;
    }

    /**
     * Register the listeners for the sensors the application is interested in.
     */
    public void enableSensor() {
        Log.i(TAG, ".enableFakeSensor() entered");
        if (isEnabled == false) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new FakeSensorTimerTask(), 1000, TIMER_PERIOD);
            isEnabled = true;
        }
    }

    /**
     * Disable the listeners.
     */
    public void disableSensor() {
        Log.d(TAG, ".disableFakeSensor() entered");
        if (timer != null && isEnabled) {
            timer.cancel();
            isEnabled = false;
        }

    }

    public int generateInitValue() {
        return randInt(MIN_VALUE, MAX_VALUE);
    }

    public void incrementValue() {
        int newValue = sensorValue + randInt(MIN_INC_VALUE, MAX_INC_VALUE);
        if (newValue > MAX_VALUE) {
            sensorValue = newValue - MAX_VALUE;
        } else {
            sensorValue = newValue;
        }
        app.setSensorValue(sensorValue);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("data", sensorValue);
        editor.commit();
    }

    public int randInt(int min, int max) {
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    public int getSensorValue() {
        return sensorValue;
    }

    /**
     * Timer task for update the fake sensor data on 10000ms intervals
     */
    private class FakeSensorTimerTask extends TimerTask {

        /**
         * Update the sensor data
         */
        @Override
        public void run() {
            Log.v(TAG, "FakeSensorTimerTask.run() entered");
            instance.incrementValue();
        }
    }
}
