package de.htwdd.industrialscan;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.entity.StringEntity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Class for calling the REST Backend and configuration of the config file.
 */
public class RestClient
{
    /**
     * This is our personal URL for REST-client-calls.
     */
    private static String BASE_URL = "http://http-server.qzcz876cdyam6dnu.myfritz.net:8080/RestWSProject/Rest/";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        readURLfromFile();
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(Context context, String url, StringEntity entity, AsyncHttpResponseHandler responseHandler)
    {
            readURLfromFile();
            client.addHeader("Content-Type", "application/json");
            client.post(context, getAbsoluteUrl(url), entity, "application/json", responseHandler);
    }

    /**
     * This call is used to change the REST-URL in the FIle-System of the mobile-phone.
     * TO setup an URL inside the app is not on purpose.
     */
    private static void readURLfromFile() {
        String filename = "IndustrialURL.cfg";
        String internal_sd = "sdcard";
        //Used for different device descriptions on different system-versions
        if(new File("sdcard0").listFiles() != null)
            internal_sd = "sdcard0";

        File files = new File("/mnt/" + internal_sd +"/Android/data/de.htwdd.industrialscan/files/" + filename);

        if (files.isFile()) {
            try {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(files)));
                String inputString2;
                StringBuffer stringBuffer = new StringBuffer();
                while ((inputString2 = inputReader.readLine()) != null) {
                    stringBuffer.append(inputString2);
                }
                BASE_URL = stringBuffer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            try{
                // Path of the config-file
                new File("/mnt/" + internal_sd +"/Android/data/de.htwdd.industrialscan/files/").mkdirs();
                BufferedWriter ouputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(files)));
                ouputWriter.append(BASE_URL);
                ouputWriter.flush();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private static String getAbsoluteUrl(String relativeUrl)
    {
        return BASE_URL + relativeUrl;
    }
}
