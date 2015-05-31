package de.htwdd.industrialscan.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Simon on 30.05.15.
 */
public class History
{
    String userid;
    String time;
    String action;

    public History(String userId) {
        this.userid = userId;
    }

    public String getUserId() {
        return userid;
    }

    public void setUserId(String userId) {
        this.userid = userId;
    }

    public String getTime()
    {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        DateFormat germanFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
        try
        {
            Date oldTime = format.parse(this.time);
            return germanFormat.format(oldTime);
        } catch (ParseException e)
        {
            e.printStackTrace();
        }
        return time.toString();
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public String getAction() {
        return action;
    }

    public String getGermanAction()
    {
        switch(this.action)
        {
            case "login":
                return "angemeldet";
            case "logout":
                return "abgemeldet";
            default:
                return action;
        }
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "History{" +
                "userId='" + userid + '\'' +
                ", time='" + time + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
