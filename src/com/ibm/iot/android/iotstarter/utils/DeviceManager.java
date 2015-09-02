package com.ibm.iot.android.iotstarter.utils;

import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.provider.Settings.Secure;

import com.ibm.iot.android.iotstarter.fragments.LoginFragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by fnegre on 8/27/2015.
 */
public class DeviceManager extends AsyncTask<String, Integer, Long> {

    public void setLoginFragment(LoginFragment lf) {
        this.loginFragment = lf;
    }

    private LoginFragment loginFragment;
    private String orgId;
    private String apiKey;
    private String apiToken;

    public Long createDevice(String id) throws Exception {
        String url = "https://internetofthings.ibmcloud.com/api/v0001/organizations/"+orgId+"/devices";
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        String userpass = apiKey + ":" + apiToken;
        System.out.println("orgId: " + orgId);
        System.out.println("API credentials: " + userpass);
        String basicAuth = "Basic " + new String(Base64.encode(userpass.getBytes(), Base64.DEFAULT));
        String type = "application/json";

        System.out.println("Authorization "+ basicAuth);

        con.setRequestProperty("Authorization", basicAuth);

        //add reuqest header
        con.setRequestMethod("POST");

        String rawData = "{\"type\": \"device\", \"id\": \""+id+"\"}";
        String encodedData = URLEncoder.encode(rawData, "UTF-8");

        System.out.println("### encodedData "+encodedData);

        // Send post request
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", type);
        con.setRequestProperty( "Content-Length", String.valueOf(rawData.length()));

        DataOutputStream wr = new DataOutputStream(con.getOutputStream());

        wr.writeBytes(rawData);
        wr.flush();
        wr.close();



//        String rawData = "id=10";
//        String type = "application/x-www-form-urlencoded";
//        String encodedData = URLEncoder.encode( rawData );
//        URL u = new URL("http://www.example.com/page.php");
//        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
//        conn.setDoOutput(true);
//        conn.setRequestMethod("POST");
//        conn.setRequestProperty( "Content-Type", type );
//        conn.setRequestProperty( "Content-Length", String.valueOf(encodedData.length()));
//        OutputStream os = conn.getOutputStream();
//        os.write(encodedData.getBytes());



        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + rawData);
        System.out.println("Response Code : " + responseCode);

        if (responseCode != 403 && responseCode != 401) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());

            JSONObject jsonObject = new JSONObject(response.toString());
            String deviceId = jsonObject.getString("id");
            String deviceToken = jsonObject.getString("password");
            loginFragment.saveAndconnect(deviceId, deviceToken);
        } else if (responseCode == 409) {
            throw new RuntimeException("Device is already registered.");
        } else {
            throw new RuntimeException("Could not register device: "+responseCode);
        }

        return 100L;
    }

    @Override
    protected Long doInBackground(String... strings) {
        try {
            return createDevice(strings[0]);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
}
