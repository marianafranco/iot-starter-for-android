/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *******************************************************************************/
package com.ibm.iot.android.iotstarter.utils;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.ibm.iot.android.iotstarter.IoTStarterApplication;
import com.ibm.iot.android.iotstarter.fragments.IoTFragment;

import java.util.Timer;
import java.util.TimerTask;

/**
 * LocationUtils enables and disables location services so that the application can publish latitude
 * and longitude data.
 */
public class LocationUtils implements LocationListener {
    private final static String TAG = LocationUtils.class.getName();
    public static final int TIMER_PERIOD = 3000;

    private static LocationUtils instance;
    private IoTStarterApplication app;
    private LocationManager locationManager;
    private Context context;
    private Criteria criteria;
    private Timer timer;
    private String latestProvider;
    private int networkCycles = 0;

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            System.out.println("### " + inputMessage.obj);
            disconnectCurrentProvider();
            getNextProvider((Boolean)inputMessage.obj);
            connect();
        }
    };


    private LocationUtils(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.criteria = getCriteria();
        this.app = (IoTStarterApplication) context.getApplicationContext();
        getNextProvider(false);
    }

    public static LocationUtils getInstance(Context context) {
        if (instance == null) {
            instance = new LocationUtils(context);
        }
        return instance;
    }


    private String getNextProvider(boolean force) {
        String locationProvider;

        boolean isGPSProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSProviderEnabled) {
            Log.i(TAG, "GPS Location provider not enabled");
        }
        if (!isNetworkProviderEnabled) {
            Log.i(TAG, "Network Location provider not enabled");
        }

        if (isGPSProviderEnabled && !isNetworkProviderEnabled) {
            locationProvider = LocationManager.GPS_PROVIDER;
        } else if (!isGPSProviderEnabled && isNetworkProviderEnabled) {
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            if (force) {
                if (LocationManager.GPS_PROVIDER.equals(latestProvider)) {
                    locationProvider = LocationManager.NETWORK_PROVIDER;
                } else {
                    locationProvider = LocationManager.GPS_PROVIDER;
                }
            } else {
                locationProvider = LocationManager.GPS_PROVIDER;
            }
        }
        latestProvider = locationProvider;

        return locationProvider;
    }

    /**
     * Enable location services
     */
    public void connect() {
        Log.i(TAG, ".connect() entered");
        System.out.println("### connect");

        // Check if location provider is enabled
        String locationProvider = latestProvider;

        app.setCurrentLocationProvider(""+locationProvider.toUpperCase());
        String runningActivity = app.getCurrentRunningActivity();
        if (runningActivity != null && runningActivity.equals(IoTFragment.class.getName())) {
            Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
            actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_LOCATION_PR);
            context.sendBroadcast(actionIntent);
        }

        if (locationManager.isProviderEnabled(locationProvider) == false) {
            Log.i(TAG, "Location provider not enabled: " + locationProvider);
            app.setCurrentLocation(null);
            return;
        }

        // register for location updates
        String bestProvider = locationManager.getBestProvider(criteria, false);
//        locationManager.requestLocationUpdates(bestProvider, Constants.LOCATION_MIN_TIME, Constants.LOCATION_MIN_DISTANCE, this);
        locationManager.requestLocationUpdates(bestProvider, 1000, 0, this);
        app.setCurrentLocation(locationManager.getLastKnownLocation(locationProvider));

        // start timer
        if (timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new SendLocationTimerTask(), 1000, TIMER_PERIOD);
        }
    }



    /**
     * Disable location services
     */
    public void disconnect() {
        Log.d(TAG, ".disconnect() entered");

        disconnectCurrentProvider();

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void disconnectCurrentProvider() {
        if (locationManager.isProviderEnabled(latestProvider)) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, ".onLocationChanged() entered");

        //publish location details
        app.setCurrentLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, ".onStatusChanged() entered");

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, ".onProviderEnabled() entered");

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, ".onProviderDisabled() entered");

    }

    /**
     * Helper method to create a criteria for location change listener
     *
     * @return criteria constructed for the listener
     */
    public Criteria getCriteria() {
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setSpeedRequired(false);
        return criteria;
    }

    /**
     * Timer task for sending location data on 3000ms intervals
     */
    private class SendLocationTimerTask extends TimerTask {

        /**
         * Publish an location event message.
         */
        @Override
        public void run() {
            Log.v(TAG, "SendLocationTimerTask.run() entered");

            double lon = 0.0;
            double lat = 0.0;
            Location currentLocation = app.getCurrentLocation();
            String initialProvider = latestProvider;

            if (null == currentLocation) {
//                disconnectCurrentProvider();
//                getNextProvider(true);
//                connect();
                System.out.println("Trying to use other provider than "+latestProvider);
                Message m = mHandler.obtainMessage(0, Boolean.TRUE);
                mHandler.sendMessage(m);
                currentLocation = app.getCurrentLocation();
            }

            if (currentLocation == null) {
                Log.e(TAG, "Could not retrieve current location using provider "+latestProvider);
            } else {
                lon = currentLocation.getLongitude();
                lat = currentLocation.getLatitude();
                if (!LocationManager.GPS_PROVIDER.equals(initialProvider) &&
                        !LocationManager.GPS_PROVIDER.equals(latestProvider) &&
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        (networkCycles++) > 60000/TIMER_PERIOD) {
                    networkCycles = 0;
                    System.out.println("Trying to use GPS provider");
                    // change to GPS since it is enabled.
//                    disconnectCurrentProvider();
//                    getNextProvider(false);
//                    connect();
                    Message m = mHandler.obtainMessage(0, Boolean.FALSE);
                    mHandler.sendMessage(m);
                }
            }

            if (lon != 0.0 && lat != 0.0) {
                String deviceId = app.getDeviceId();
                int sensorValue = app.getSensorValue();
                String messageData = MessageFactory.getLocationMessage(deviceId, lon, lat, sensorValue);
                String topic = TopicFactory.getEventTopic(Constants.LOCATION_EVENT);
                MqttHandler mqttHandler = MqttHandler.getInstance(context);
                mqttHandler.publish(topic, messageData, false, 0);

                String runningActivity = app.getCurrentRunningActivity();
                if (runningActivity != null && runningActivity.equals(IoTFragment.class.getName())) {
                    Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
                    actionIntent.putExtra(Constants.INTENT_DATA, Constants.LOCATION_EVENT);
                    context.sendBroadcast(actionIntent);
                }
            }
        }

    }
}
