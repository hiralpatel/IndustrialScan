package de.htwdd.industrialscan;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.htwdd.industrialscan.model.Person;

/**
 * Created by Simon on 24.05.15.
 */
public class HistoryFragment extends Fragment
{
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    TextView tv;
    ListView lv;
    String s="";


    /**
     * Returns a new instance of this fragment for the history list screen
     * number.
     */
    public static HistoryFragment newInstance(int sectionNumber)
    {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public HistoryFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);
        tv=(TextView) rootView.findViewById(R.id.textView);
        lv=(ListView) rootView.findViewById(R.id.historyListView);

        String value[]={"Hallo! Klick mich um den REST Call auszuführen",};
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,android.R.id.text1,value);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3)
            {
                try
                {
                    //Possible Rest Calls:

                    //show all persons in list:
                    //getAllPersons();

                    //show only matthias in list:
                    getPersonById("1293A893");

                } catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });
        return rootView;
    }

    public void getAllPersons() throws JSONException
    {
        RestClient.get("users/getAllPersons/", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                Gson gson = new Gson();
                // Pull out the first event on the public timeline
                JSONObject firstEvent = null;
                String tweetText = "";
                Person[] persons = gson.fromJson(timeline.toString(), Person[].class);
                String value[] = new String[persons.length];
                for (int i = 0; i < persons.length; i++) {
                    value[i] = persons[i].toString();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, value);
                lv.setAdapter(adapter);
            }
        });
    }

    public void getPersonById(String id) throws JSONException
    {
        RestClient.get("users/getPersonByIdJSON/"+id, null, new JsonHttpResponseHandler()
        {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline)
            {
                Gson gson = new Gson();
                // Pull out the first event on the public timeline
                JSONObject firstEvent = null;
                String tweetText = "";
                Person[] persons = gson.fromJson(timeline.toString(), Person[].class);
                String value[] = new String[persons.length];
                for (int i = 0; i < persons.length; i++) {
                    value[i] = persons[i].toString();
                }
                ArrayAdapter<String> adapter=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,android.R.id.text1,value);
                lv.setAdapter(adapter);
            }
        });
    }
}