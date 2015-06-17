package de.htwdd.industrialscan;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import de.htwdd.industrialscan.model.CameraPreview;
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

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    static ViewPager mViewPager;

    //loading ZBAr lib for Galaxy Ace
    static {
        System.loadLibrary("iconv");
    }


    /*
        Steven
     */
    private TextView scanned_rfid;
    private NfcAdapter mAdapter;
    protected PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;
    private AlertDialog mDialog;

    // Camera

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    private Uri fileUri;

    private static Camera mCamera;
    private static CameraPreview mPreview;
    private static TextView scanText;
    private static TextView scan_type;
    private static Button qr_button;
    private static FrameLayout qr_livecam;
    private static ImageView qr_spoiler;

    private static ImageScanner scanner;

    private static boolean barcodeScanned = true;
    private static boolean previewing = true;
    private static Handler autoFocusHandler;
    static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        context = getApplicationContext();

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


        /*
        Steven
         */
        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        //if (mAdapter == null) {
            //showMessage(R.string.error, R.string.no_nfc);
            //finish();
            //return;
        //}
        // Eventhandler über NFC
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        
        
    }

    @Override
    protected void onResume() {
        System.out.println("main:onResume()");
        super.onResume();
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        System.out.println("main:onNewIntent()");
        setIntent(intent);
        resolveIntent(intent);
    }

    public void resolveIntent(Intent intent){

        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            // Ladescreen

            scanned_rfid = (TextView) findViewById(R.id.scanned_rfid);

            // Umwandeln
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String hex = getHex(tag.getId());
            System.out.println("Hex-ID: " + hex);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hex.length(); i += 2) {
                sb.append(hex.substring(i, i + 2) + " ");
            }
            System.out.println("SB:Hex-ID: " + sb.toString());
            scanned_rfid.setText(sb.toString());
            System.out.println("Ausgabe1 : " + scanned_rfid.getText());
            processScannedId(hex);
            scan_type.setText("RFID :");
            scanned_rfid.setTextColor(getResources().getColor(R.color.color_white));

            // Intent intent = new Intent(this, NextActivity.class);
            // startActivity(intent);
        }
        return;
    }


    private String getHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b).toUpperCase());
        }
        String hex = sb.toString();
        String reverse = "";
        for (int i = sb.length(); i>0 ; i=i-2) {
            reverse += hex.substring(i - 2, i);
        }
        return reverse;
    }

    // Anzeigen der NFC-Einstellungen
    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.nfc_disabled);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
        return;
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
            if(position==1) return PlaceholderFragment.newInstance(0);
            else if(position==0) return HistoryFragment.newInstance(1);
            else if(position==2) return UserFragment.newInstance(2);
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

        @Override
        public void onPause() {
            super.onPause();
            System.out.println("Scan :: OnPause()");
        }

        @Override
        public void onResume() {
            super.onResume();
            System.out.println("Scan :: OnResume()");
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_scan, container, false);

            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            System.out.println("onCreate::: Scan!!!!");

            qr_button = (Button)rootView.findViewById(R.id.button_capture);
            qr_spoiler = (ImageView) rootView.findViewById(R.id.imageView2);
            qr_livecam = (FrameLayout) rootView.findViewById(R.id.imageView);
            scanText = (TextView) rootView.findViewById(R.id.scanned_rfid);
            scan_type = (TextView) rootView.findViewById(R.id.show_rfid);


            autoFocusHandler = new Handler();
            mCamera = getCameraInstance();

            /* Instance barcode scanner */

            scanner = new ImageScanner();
            scanner.setConfig(0, Config.ENABLE, 0);
            scanner.setConfig(Symbol.QRCODE, Config.ENABLE, 1);


            mPreview = new CameraPreview(inflater.getContext(), mCamera, previewCb, autoFocusCB);
            FrameLayout preview = (FrameLayout) rootView.findViewById(R.id.imageView);
            preview.addView(mPreview);




            qr_button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (barcodeScanned) {
                        barcodeScanned = false;
                        qr_button.setVisibility(View.INVISIBLE);
                        qr_spoiler.setVisibility(View.INVISIBLE);
                        qr_livecam.setVisibility(View.VISIBLE);
                        scanText.setText("## ## ## ##");
                        scan_type.setText("QR / RFID :");
                        scanText.setTextColor(Color.parseColor("#FFFFFF"));
                        scan_type.setTextColor(Color.parseColor("#FFFFFF"));

                        mCamera.setPreviewCallback(previewCb);
                        mCamera.startPreview();
                        previewing = true;
                        mCamera.autoFocus(autoFocusCB);
                    }
                }
            });
            return rootView;
        }
    }

    static Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image qrcode = new Image(size.width, size.height, "Y800");
            qrcode.setData(data);

            int result = scanner.scanImage(qrcode);

            if (result != 0) {
                previewing = false;
                //mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                qr_button.setText("QR-Code identifiziert. \n Klicken Sie für einen erneuten Scan!");
                scanText.setTextColor(Color.parseColor("#FFFFFF"));

                scan_type.setText("QR :");
                scan_type.setTextColor(Color.parseColor("#FFFFFF"));
                qr_button.setTextSize(15);
                qr_button.setVisibility(View.VISIBLE);


                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {

                    scanText.setText(sym.getData());
                    System.out.println("Ausgabe2 : " + scanText.getText());
                    processScannedId(sym.getData());
                    barcodeScanned = true;
                }
            }
        }
    };

    static public void processScannedId(final String id)
    {
        //check for if user exists
        RestClient.get("users/getPersonByIdJSON/" + id, null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Gson gson = new Gson();
                Person[] persons = gson.fromJson(response.toString(), Person[].class);
                if (persons.length != 1) {
                    qr_button.setText("Nutzer mit dieser ID ist nicht vorhanden! \n Klicken Sie für einen erneuten Scan!");
                } else //User found! Now fetch the last action of this user
                {
                    RestClient.get("users/getPersonsCurrentActionByIdJSON/" + id, null, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                            Gson gson = new Gson();
                            History[] histories = gson.fromJson(response.toString(), History[].class);
                            final History newHistory = new History(id);
                            if (histories.length == 0) {
                                newHistory.setAction("login");
                            } else if (histories[0].getAction().equals("logout")) {
                                newHistory.setAction("login");
                            } else {
                                newHistory.setAction("logout");
                            }
                            StringEntity entity = null;
                            try {
                                entity = new StringEntity(gson.toJson(newHistory));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            RestClient.post(context, "users/saveHistory/", entity, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    qr_button.setTextSize(15);
                                    if (response.toString().contains("OK")) {
                                        qr_button.setText("Sie wurden erfolgreich an dieser Maschine" + newHistory.getGermanAction() + "\n Klicken Sie für einen erneuten Scan!");
                                    } else {
                                        qr_button.setText("Bei der Authorisierung ist ein Fehler aufgetreten!\n Klicken Sie für einen erneuten Scan!");
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    // Mimic continuous auto-focusing
    static Camera.AutoFocusCallback  autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private static Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    public void onPause() {
        super.onPause();
        //releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
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

        @Override
        public void onPause() {
            super.onPause();
            System.out.println("HistoryFragment :: OnPause()");
        }

        @Override
        public void onResume() {
            super.onResume();
            System.out.println("HistoryFragment :: OnResume()");
        }

        public HistoryFragment()
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_history, container, false);
            lv=(ListView) rootView.findViewById(R.id.historyListView);

            getAllHistories();

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                        long arg3) {
                    String listEntry = (String) arg0.getItemAtPosition(arg2);
                    String userId = listEntry.substring(0, listEntry.indexOf("hat"));
                    if (!userId.isEmpty()) {
                        getPersonById(userId);
                    }
                }
            });
            return rootView;
        }

        public void getAllHistories()
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

        public void getPersonById(String id)
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
                    Toast.makeText(getActivity(), ""+persons[0].getFirstName()+ " " +persons[0].getLastName()+ " aus der Abteilung "+ persons[0].getRole() ,
                            Toast.LENGTH_LONG).show();
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
        ListView lv;

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

        @Override
        public void onPause() {
            super.onPause();
            System.out.println("User :: OnPause()");
        }

        @Override
        public void onResume() {
            super.onResume();
            System.out.println("User :: OnResume()");
        }

        public UserFragment()
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_user, container, false);
            lv=(ListView) rootView.findViewById(R.id.userListView);

            getAllPersons();

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                        long arg3) {
                    String listEntry = (String) arg0.getItemAtPosition(arg2);
                    String userId = listEntry.substring(0, listEntry.indexOf("hat"));
                    if(!userId.isEmpty())
                    {
                        Toast.makeText(context, "Nutzer" + userId, Toast.LENGTH_LONG).show();
                    }
                }
            });
            return rootView;
        }

        public void getAllPersons()
        {
            RestClient.get("users/getAllPersons/", null, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                    Gson gson = new Gson();
                    Person[] persons = gson.fromJson(timeline.toString(), Person[].class);
                    String value[] = new String[persons.length];
                    for (int i = 0; i < persons.length; i++) {
                        value[i] = persons[i].getFirstName()+" "+persons[i].getLastName();
                    }
                    ArrayAdapter<String> adapter=new UserListAdapter(getActivity(),value);
                    lv.setAdapter(adapter);
                }
            });
        }
    }
}
