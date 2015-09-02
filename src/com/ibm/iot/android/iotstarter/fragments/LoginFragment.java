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
package com.ibm.iot.android.iotstarter.fragments;

import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.ibm.iot.android.iotstarter.IoTStarterApplication;
import com.ibm.iot.android.iotstarter.R;
import com.ibm.iot.android.iotstarter.activities.MainActivity;
import com.ibm.iot.android.iotstarter.utils.Constants;
import com.ibm.iot.android.iotstarter.utils.DeviceManager;
import com.ibm.iot.android.iotstarter.utils.DeviceSensor;
import com.ibm.iot.android.iotstarter.utils.IoTDevice;
import com.ibm.iot.android.iotstarter.utils.LocationUtils;
import com.ibm.iot.android.iotstarter.utils.MqttHandler;

/**
 * The login fragment of the IoTStarter application. Provides functionality for
 * connecting to IoT. Also displays device information.
 */
public class LoginFragment extends IoTStarterFragment {
    private final static String TAG = LoginFragment.class.getName();

    /**************************************************************************
     * Fragment functions for establishing the fragment
     **************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.login, container, false);
    }

    /**
     * Called when the fragment is resumed.
     */
    @Override
    public void onResume() {
        Log.d(TAG, ".onResume() entered");

        super.onResume();
        app = (IoTStarterApplication) getActivity().getApplication();
        app.setCurrentRunningActivity(TAG);

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering loginBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for loginBroadcastReceiver");
                    processIntent(intent);
                }
            };
        }

        getActivity().getApplicationContext().registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.APP_ID + Constants.INTENT_LOGIN));

        // initialise
        initializeLoginActivity();
    }

    /**
     * Called when the fragment is destroyed.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, ".onDestroy() entered");

        try {
            getActivity().getApplicationContext().unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException iae) {
            // Do nothing
        }
        super.onDestroy();
    }

    /**
     * Initializing onscreen elements and shared properties
     */
    private void initializeLoginActivity() {
        Log.d(TAG, ".initializeLoginFragment() entered");

        context = getActivity().getApplicationContext();

        updateViewStrings();

        // setup button listeners
        initializeButtons();
    }

    /**
     * Update strings in the fragment based on IoTStarterApplication values.
     */
    @Override
    protected void updateViewStrings() {
        Log.d(TAG, ".updateViewStrings() entered");
        // Update only if the organization is set to some non-empty string.
        if (app.getOrganization() != null) {
            ((EditText) getActivity().findViewById(R.id.organizationValue)).setText(app.getOrganization());
        } else {
            ((EditText) getActivity().findViewById(R.id.organizationValue)).setText("yi2gix");
        }

        // DeviceId should never be null at this point.
        if (app.getDeviceId() != null) {
            ((EditText) getActivity().findViewById(R.id.apiKeyValue)).setText(app.getApiKey());
        } else {
            ((EditText) getActivity().findViewById(R.id.apiKeyValue)).setText("a-yi2gix-z0ni6ws4df");
        }

        if (app.getAuthToken() != null) {
            ((EditText) getActivity().findViewById(R.id.apiTokenValue)).setText(app.getApiToken());
        } else {
            ((EditText) getActivity().findViewById(R.id.apiTokenValue)).setText("I87eKuBM@Ez2*ZNVrz");
        }

        // Set 'Connected to IoT' to Yes if MQTT client is connected. Leave as No otherwise.
        if (app.isConnected()) {
            processConnectIntent();
        }

        int unreadCount = app.getUnreadCount();
        ((MainActivity) getActivity()).updateBadge(getActivity().getActionBar().getTabAt(2), unreadCount);
    }

    /**
     * Setup listeners for buttons.
     */
    private void initializeButtons() {
        Log.d(TAG, ".initializeButtons() entered");

        Button button = (Button) getActivity().findViewById(R.id.showTokenButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleShowToken();
            }
        });

        button = (Button) getActivity().findViewById(R.id.activateButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleActivate();
            }
        });
    }

    /**************************************************************************
     * Functions to handle button presses
     **************************************************************************/

    /**
     * Check whether the required properties are set for the app to connect to IoT.
     *
     * @return True if properties are set, false otherwise.
     */
    private boolean checkCanConnect() {
        if (app.getOrganization().equals(Constants.QUICKSTART)) {
            app.setConnectionType(Constants.ConnectionType.QUICKSTART);
            if (app.getDeviceId() == null || app.getDeviceId().equals("")) {
                return false;
            }
        } else if (app.getOrganization().equals(Constants.M2M)) {
            app.setConnectionType(Constants.ConnectionType.M2M);
            if (app.getDeviceId() == null || app.getDeviceId().equals("")) {
                return false;
            }
        } else {
            app.setConnectionType(Constants.ConnectionType.IOTF);
            if (app.getOrganization() == null || app.getOrganization().equals("") ||
                    app.getDeviceId() == null || app.getDeviceId().equals("") ||
                    app.getAuthToken() == null || app.getAuthToken().equals("")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Display alert dialog indicating what properties must be set in order to connect to IoT.
     */
    private void displaySetPropertiesDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.connect_props_title))
                .setMessage(getResources().getString(R.string.connect_props_text))
                .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
    }

    /**
     * If button is currently 'Activate', then connect the MQTT client.
     * If button is currently 'Deactivate', then disconnect the MQTT client.
     */
    private void handleActivate() {
        Log.d(TAG, ".handleActivate() entered");
        String buttonTitle = ((Button) getActivity().findViewById(R.id.activateButton)).getText().toString();
        MqttHandler mqttHandle = MqttHandler.getInstance(context);
        Button activateButton = (Button) getActivity().findViewById(R.id.activateButton);
        String orgId = ((EditText) getActivity().findViewById(R.id.organizationValue)).getText().toString();
        String apiKey = ((EditText) getActivity().findViewById(R.id.apiKeyValue)).getText().toString();
        String apiToken = ((EditText) getActivity().findViewById(R.id.apiTokenValue)).getText().toString();
        app.setOrganization(orgId);
        activateButton.setEnabled(false);

        try {
            IoTDevice device = null;
            for (IoTDevice ioTDevice: app.getDeviceSettings()) {
                if (ioTDevice.getProfileName().equals(Constants.DEVICE_SETTINGS + orgId)) {
                    device = ioTDevice;
                }
            }

            if (device != null) {
                System.out.println("Using previously created device id for organization "+orgId);
                connect(device.getDeviceID(), device.getAuthorizationToken());
            } else {
                String android_id = Settings.Secure.getString(getActivity().getApplicationContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                DeviceManager deviceManager = new DeviceManager();
                deviceManager.setOrgId(orgId);
                deviceManager.setApiKey(apiKey);
                deviceManager.setApiToken(apiToken);
                deviceManager.setLoginFragment(this);
                deviceManager.execute(android_id);
            }
        } catch (Exception e) {
            System.out.println(e);
            app.setConnected(false);
            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null && runningActivity.equals(LoginFragment.class.getName())) {
                Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
                actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_DISCONNECT);
                context.sendBroadcast(actionIntent);
            }
            e.printStackTrace();
        }
    }

    public void connect(String deviceId, String authToken) {
        String buttonTitle = ((Button) getActivity().findViewById(R.id.activateButton)).getText().toString();
        MqttHandler mqttHandle = MqttHandler.getInstance(context);
        Button activateButton = (Button) getActivity().findViewById(R.id.activateButton);

        app.setDeviceId(deviceId);
        app.setAuthToken(authToken);

        if (buttonTitle.equals(getResources().getString(R.string.activate_button)) && app.isConnected() == false) {
            if (checkCanConnect()) {
                mqttHandle.connect();
            } else {
                displaySetPropertiesDialog();
                activateButton.setEnabled(true);
            }
        } else if (buttonTitle.equals(getResources().getString(R.string.deactivate_button)) && app.isConnected() == true) {
            mqttHandle.disconnect();
        }
    }

    public void saveAndconnect(String deviceId, String authToken) {
        IoTDevice device = new IoTDevice(Constants.DEVICE_SETTINGS + app.getOrganization(), app.getOrganization(), deviceId, authToken);
        app.saveDeviceSetting(device);
        connect(deviceId, authToken);
    }

    /**
     * Toggle auth token text field secure text entry
     */
    private void handleShowToken() {
        Log.d(TAG, ".handleShowToken() entered");
        Button showTokenButton = (Button) getActivity().findViewById(R.id.showTokenButton);
        String buttonTitle = showTokenButton.getText().toString();
        EditText tokenText = (EditText) getActivity().findViewById(R.id.apiTokenValue);
        if (buttonTitle.equals(getResources().getString(R.string.showToken_button))) {
            showTokenButton.setText(getResources().getString(R.string.hideToken_button));
            tokenText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else if (buttonTitle.equals(getResources().getString(R.string.hideToken_button))) {
            showTokenButton.setText(getResources().getString(R.string.showToken_button));
            tokenText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    /**************************************************************************
     * Functions to process intent broadcasts from other classes
     **************************************************************************/

    /**
     * Process the incoming intent broadcast.
     *
     * @param intent The intent which was received by the fragment.
     */
    private void processIntent(Intent intent) {
        Log.d(TAG, ".processIntent() entered");

        // No matter the intent, update log button based on app.unreadCount.
        updateViewStrings();

        String data = intent.getStringExtra(Constants.INTENT_DATA);
        assert data != null;
        if (data.equals(Constants.INTENT_DATA_CONNECT)) {
            processConnectIntent();
            openIoT();
        } else if (data.equals(Constants.INTENT_DATA_DISCONNECT)) {
            processDisconnectIntent();
        } else if (data.equals(Constants.ALERT_EVENT)) {
            String message = intent.getStringExtra(Constants.INTENT_DATA_MESSAGE);
            new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(R.string.alert_dialog_title))
                    .setMessage(message)
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).show();
        }
    }

    /**
     * Intent data contained INTENT_DATA_CONNECT.
     * Update Connected to Yes.
     */
    private void processConnectIntent() {
        Log.d(TAG, ".processConnectIntent() entered");
        Button activateButton = (Button) getActivity().findViewById(R.id.activateButton);
        activateButton.setEnabled(true);
        String connectedString = this.getString(R.string.is_connected);
        connectedString = connectedString.replace("No", "Yes");
        ((TextView) getActivity().findViewById(R.id.isConnected)).setText(connectedString);
        activateButton.setText(getResources().getString(R.string.deactivate_button));
//        if (app.isAccelEnabled()) {
//            LocationUtils locUtils = LocationUtils.getInstance(context);
//            locUtils.connect();
//            app.setDeviceSensor(DeviceSensor.getInstance(context));
//            app.getDeviceSensor().enableSensor();
//        }
        LocationUtils locUtils = LocationUtils.getInstance(context);
        locUtils.connect();
    }

    /**
     * Intent data contained INTENT_DATA_DISCONNECT.
     * Update Connected to No.
     */
    private void processDisconnectIntent() {
        Log.d(TAG, ".processDisconnectIntent() entered");
        Button activateButton = (Button) getActivity().findViewById(R.id.activateButton);
        activateButton.setEnabled(true);
        ((TextView) getActivity().findViewById(R.id.isConnected)).setText(this.getString(R.string.is_connected));
        activateButton.setText(getResources().getString(R.string.activate_button));
//        if (app.getDeviceSensor() != null && app.isAccelEnabled()) {
//            LocationUtils locUtils = LocationUtils.getInstance(context);
//            app.getDeviceSensor().disableSensor();
//            if (locUtils != null) {
//                locUtils.disconnect();
//            }
//        }
        LocationUtils locUtils = LocationUtils.getInstance(context);
        if (locUtils != null) {
            locUtils.disconnect();
        }
    }

}