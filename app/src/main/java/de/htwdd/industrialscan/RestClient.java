package de.htwdd.industrialscan;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.entity.StringEntity;
import org.json.JSONException;

/**
 * Class for calling the REST Backend
 * Created by Simon on 27.05.15.
 */
public class RestClient
{
    private static final String BASE_URL = "http://http-server.qzcz876cdyam6dnu.myfritz.net:8080/RestWSProject/Rest/";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(Context context, String url, StringEntity entity, AsyncHttpResponseHandler responseHandler)
    {
        client.addHeader("Content-Type","application/json");
        client.post(context,getAbsoluteUrl(url),entity,"application/json",responseHandler);
    }



    private static String getAbsoluteUrl(String relativeUrl)
    {
        return BASE_URL + relativeUrl;
    }
}
