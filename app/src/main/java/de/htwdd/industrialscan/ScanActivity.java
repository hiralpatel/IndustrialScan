package de.htwdd.industrialscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.Header;
import org.json.*;

import com.google.gson.Gson;
import com.loopj.android.http.*;

import de.htwdd.industrialscan.model.History;
import de.htwdd.industrialscan.model.Person;


public class ScanActivity extends ActionBarActivity implements ActionBar.TabListener
{
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    static Person currentlySelectedPerson;
    SectionsPagerAdapter mSectionsPagerAdapter;
    private List historyItemList; // at the top of your fragment list

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    static ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++)
        {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if(position==0) return PlaceholderFragment.newInstance(1);
            else if(position==1) return HistoryFragment.newInstance(2);
            else if(position==2) return UserFragment.newInstance(3);
            else return new Fragment();
        }

        @Override
        public int getCount()
        {
            // Show 3 total pages for scroll view.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
    {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_scan, container, false);
            return rootView;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class HistoryFragment extends Fragment
    {
        /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

        ListView lv;

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
            lv=(ListView) rootView.findViewById(R.id.historyListView);

            try
            {
                getAllHistories();
            } catch (JSONException e)
            {
                e.printStackTrace();
            }

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                        long arg3) {
                    String listEntry = (String) arg0.getItemAtPosition(arg2);
                    String userId = listEntry.substring(0, listEntry.indexOf("hat"));
                    if(!userId.isEmpty())
                    {
                        try
                        {
                            getPersonById(userId);
                        } catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
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

        public void getAllHistories() throws JSONException
        {
            RestClient.get("users/getAllHistories/", null, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                    Gson gson = new Gson();
                    History[] histories = gson.fromJson(timeline.toString(), History[].class);
                    String value[] = new String[histories.length];
                    for (int i = 0; i < histories.length; i++)
                    {
                        value[i] = histories[i].getUserId() + " hat sich am " + histories[i].getTime() + " Uhr " + histories[i].getGermanAction();
                    }
                    ArrayAdapter<String> adapter=new HistoryListAdapter(getActivity(),value);
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
                    Person[] persons = gson.fromJson(timeline.toString(), Person[].class);
                    String value[] = new String[persons.length];
                    for (int i = 0; i < persons.length; i++) {
                        value[i] = persons[i].toString();
                    }
                    currentlySelectedPerson = persons[0];
                    mViewPager.setCurrentItem(3);
                }
            });
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class UserFragment extends Fragment
    {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        TextView name;

        /**
         * Returns a new instance of this fragment for the history list screen
         * number.
         */
        public static UserFragment newInstance(int sectionNumber)
        {
            UserFragment fragment = new UserFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public UserFragment()
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_user, container, false);
            name = (TextView) rootView.findViewById(R.id.textViewNameContent);
            if(currentlySelectedPerson!=null) name.setText(currentlySelectedPerson.getFirstName());
            return rootView;
        }
    }
}
