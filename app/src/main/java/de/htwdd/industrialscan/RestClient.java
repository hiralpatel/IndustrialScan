package de.htwdd.industrialscan;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.entity.StringEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Class for calling the REST Backend
 * Created by Simon on 27.05.15.
 */
public class RestClient
{
    private static String BASE_URL = "http://http-server.qzcz876cdyam6dnu.myfritz.net:8080/RestWSProject/Rest/";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        readURLfromFile();
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" + BASE_URL);
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(Context context, String url, StringEntity entity, AsyncHttpResponseHandler responseHandler)
    {
        readURLfromFile();
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<" + BASE_URL);
        client.addHeader("Content-Type","application/json");
        client.post(context,getAbsoluteUrl(url),entity,"application/json",responseHandler);
    }

    private static void readURLfromFile() {
        File storageDir = new File("/mnt/extSdCard/Android/data/de.htwdd.industrialscan/files/IndustrialURL.cfg");
        if (storageDir.isFile()) {
            try {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(storageDir)));
                String inputString2;
                StringBuffer stringBuffer = new StringBuffer();
                while ((inputString2 = inputReader.readLine()) != null) {
                    stringBuffer.append(inputString2);
                }
                BASE_URL = stringBuffer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String getAbsoluteUrl(String relativeUrl)
    {
        return BASE_URL + relativeUrl;
    }
}
