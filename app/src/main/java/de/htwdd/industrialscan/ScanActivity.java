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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.htwdd.industrialscan.model.CameraPreview;
import de.htwdd.industrialscan.model.History;
import de.htwdd.industrialscan.model.Person;

/**
 * This is the main-activity. It implements every bar of the app, the scan analysis logic and also
 * the animation logic.
 */
public class ScanActivity extends ActionBarActivity implements ActionBar.TabListener
{

    static Person currentlySelectedPerson;
    SectionsPagerAdapter mSectionsPagerAdapter;

    // This will host the section contents.
    static ViewPager mViewPager;

    /**
     * Try to load the ZBAr lib for Galaxy Ace.
     */
    static {
        System.loadLibrary("iconv");
    }

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    private Uri fileUri;
    private TextView scanned_rfid;
    private NfcAdapter mAdapter;
    protected PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;
    private AlertDialog mDialog;
    private static Camera mCamera;
    private static CameraPreview mPreview;
    private static TextView scanText;
    private static TextView scan_type;
    private static Button qr_button;
    private static FrameLayout qr_livecam;
    private static ImageView qr_spoiler;
    private static Context toastContext;
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
        setContext(context);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        /**
         * Create the adapter that will return a fragment for each of the three
         * primary sections of the activity.
         */

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

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();
        mAdapter = NfcAdapter.getDefaultAdapter(this);

        // Eventhandler for NFC
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    /**
     * Used to recall the app while stopped. Needed to recapture the camera-stream.
     */

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    /**
     * The method is used to get the event of an RFID event
     * @param intent RFID intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }

    /**
     * The method reverse the rfid hex code and call's back-/front-end.
     * @param intent
     */
    public void resolveIntent(Intent intent){

        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            scanned_rfid = (TextView) findViewById(R.id.scanned_rfid);

            // Convert Hex ID
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String hex = getHex(tag.getId());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hex.length(); i += 2) {
                sb.append(hex.substring(i, i + 2));
            }
            scanned_rfid.setText(sb.toString());
            processScannedId(hex);
            scan_type.setText("RFID :");
            scanned_rfid.setTextColor(getResources().getColor(R.color.color_white));
        }
        return;
    }

    /**
     * The method is used to decode the NFC-Tag to RFID.
     * @param bytes describes the byte-stream of the NFC-Tag
     * @return the RFID-Code
     */
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


    /**
     * The method will show NFC - Settings on app-start, if the service is offline.
     */

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
    }

    /**
     * The method inflates the menu. This will add items to the action bar, if it is in the
     * present case.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        return true;
    }

    /**
     * Handle action bar item clicks here. The action bar will
     * automatically handle clicks on the Home/Up button, so long
     * as you specify a parent activity in AndroidManifest.xml.
     *
     * @param item
     * @return
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //
        //
        //
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * When the given tab is selected, switch to the corresponding page in the ViewPager.
     * @param tab
     * @param fragmentTransaction
     */
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    /**
     * not used
     * @param tab
     * @param fragmentTransaction
     */
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
    }

    /**
     * not used
     * @param tab
     * @param fragmentTransaction
     */
    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
    }

    /**
     * The FragmentPagerAdapter that returns a fragment corresponding to
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
            // Return a ScanFragment (defined as a static inner class below).
            if(position==1) return ScanFragment.newInstance(0);
            else if(position==0) return HistoryFragment.newInstance(1);
            else if(position==2) return UserFragment.newInstance(2);
            else return new Fragment();
        }

        /**
         * Show 3 total pages for scroll view.
         * @return
         */
        @Override
        public int getCount()
        {
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
     * Fragment containing the QR and RFID Scan view.
     */
    public static class ScanFragment extends Fragment
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
        public static ScanFragment newInstance(int sectionNumber) {
            ScanFragment fragment = new ScanFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        public ScanFragment() {
        }

        /**
         * The method initialize the camera (QR Code-Scanner Logic). With the scan-button
         * the scan process starts.
         * @param inflater
         * @param container
         * @param savedInstanceState
         * @return
         */
        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_scan, container, false);


            qr_button = (Button)rootView.findViewById(R.id.button_capture);
            qr_spoiler = (ImageView) rootView.findViewById(R.id.imageView2);
            qr_livecam = (FrameLayout) rootView.findViewById(R.id.imageView);
            scanText = (TextView) rootView.findViewById(R.id.scanned_rfid);
            scan_type = (TextView) rootView.findViewById(R.id.show_rfid);

            // Used to force proper camera-focus by repeating.
            autoFocusHandler = new Handler();
            mCamera = getCameraInstance();

            // Instance barcode scanner
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

    /**
     * The method is used to set the actual context.
     * @param context
     */
    static void setContext(Context context){
        toastContext = context;
    }

    /**
     * The method is used to get the actual context.
     * @return
     */
    static Context getContext(){
        return toastContext;
    }

    /**
     * The method is used to identify a proper QR-Code. If the process find a QR-Code, it will
     * decode the result via zbar-libary-functions. It will also call the front-/backend.
     */
    static Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image qrcode = new Image(size.width, size.height, "Y800");
            qrcode.setData(data);

            int result = scanner.scanImage(qrcode);

            if (result != 0) {
                previewing = false;
                mCamera.stopPreview();
                scanText.setTextColor(Color.parseColor("#FFFFFF"));

                scan_type.setText("QR :");
                scan_type.setTextColor(Color.parseColor("#FFFFFF"));
                qr_button.setTextSize(20);
                qr_button.setVisibility(View.VISIBLE);
                qr_spoiler.setVisibility(View.VISIBLE);
                qr_livecam.setVisibility(View.INVISIBLE);


                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {

                    scanText.setText(sym.getData());
                    processScannedId(sym.getData());
                    barcodeScanned = true;
                }
            }

        }
    };

    /**
     * The methods updates the backend. It transforms the QR-Code into a json and send it
     * via REST to backend.
     * @param id
     */
    static public void processScannedId(final String id)
    {
        //check for if user exists
        RestClient.get("users/getPersonByIdJSON/" + id, null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Gson gson = new Gson();
                Person[] persons = gson.fromJson(response.toString(), Person[].class);
                if (persons.length != 1) {
                    qr_button.setText("Klicken Sie für einen erneuten Scan!");
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
                                    qr_button.setTextSize(20);
                                    if (response.toString().contains("OK")) {
                                        Toast.makeText(getContext(), "Sie wurden erfolgreich an dieser Maschine " + newHistory.getGermanAction() + "." ,
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        Exception e = new Exception("Während der Authorisierung ist ein Fehler aufgetreten !");
                                        //Toast.makeText(getContext(),"Während der Authorisierung ist ein Fehler aufgetreten !",
                                        //        Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    /**
     * Mimic continuous auto-focusing
     */
    static Camera.AutoFocusCallback  autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    /**
     * The method is used to release the camera-stream for other app while in pause.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * The method is used to do autoFocus on Camera.
     */
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

    /**
     * A safe way to get an instance of the Camera object.
     * @return null if camera is unavailable, otherwise an camera object
     */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c;
    }

    /**
     * The device will be checked for a camera.
     * @param context actual contect of the app
     * @return true/false
     */
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
     * Fragment containing the history list view.
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
         * Returns a new instance of this fragment for the section
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
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        public HistoryFragment()
        {
        }

        /**
         * Resolves the person information by ID.
         * @param inflater
         * @param container
         * @param savedInstanceState
         * @return
         */
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

        /**
         * The method updates the User-History.
         */
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
     * Fragment containing the user list view.
     */
    public static class UserFragment extends Fragment
    {
        // The fragment argument representing the section number for this fragment.
        private static final String ARG_SECTION_NUMBER = "section_number";
        ListView lv;

        /**
         * Returns a new instance of this fragment for the section number.
         * @param sectionNumber of the fragment
         * @return
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
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        public UserFragment(){}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_user, container, false);
            lv = (ListView) rootView.findViewById(R.id.userListView);
            getAllPersons();
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                }
            });
            return rootView;
        }

        public void getAllPersons(){
            RestClient.get("users/getAllPersons/", null, new JsonHttpResponseHandler()
            {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response)
                {
                Gson gson = new Gson();
                final Person[] persons = gson.fromJson(response.toString(), Person[].class);
                final List<String> values = new ArrayList<String>();
                for (final Person p : persons){
                    values.add(p.getFirstName()+" "+p.getLastName()+" ist "+p.getGermanStatus());
                }
                while(persons.length != values.size()){}
                ArrayAdapter<String> adapter=new UserListAdapter(getActivity(),values);
                lv.setAdapter(adapter);
                }
            });
        }
    }
}
