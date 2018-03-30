package com.techhive.smartdrive.Activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dekoservidoni.omfm.OneMoreFabMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;
import com.techhive.smartdrive.Problems.ReportProblemActivity;
import com.techhive.smartdrive.R;
import com.techhive.smartdrive.Speed.Accelerationlist;
import com.techhive.smartdrive.Speed.Data;
import com.techhive.smartdrive.Speed.GpsServices;
import com.techhive.smartdrive.Trackers.FileDownTryActivity;
import com.techhive.smartdrive.Utilities.SharedPrefManager;

import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class NavActivity extends AppCompatActivity implements LocationListener, GpsStatus.Listener,
        OnMapReadyCallback ,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private OneMoreFabMenu oneMoreFabMenu;
    private NavigationView navigationView;
    private TextView mFullNameTextView, mEmailTextView;
    private CircleImageView mProfileImageView;
    String longi,lati;
    private LocationManager mLocationManager;
    private static Data data;
    TextView Speedtextview;
    Accelerationlist a;
    private FirebaseAuth auth;
    public SharedPrefManager sharedPrefManager;
    Context mContext = this;
    private Data.onGpsServiceUpdate onGpsServiceUpdate;
    private boolean firstfix;
    private SharedPreferences sharedPreferences;
    SupportMapFragment supportMapFragment;
    GoogleMap googleMap;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initNavigationDrawer();
        initFabbutton();
        auth = FirebaseAuth.getInstance();
        View header = navigationView.getHeaderView(0);
        mFullNameTextView = (TextView) header.findViewById(R.id.fullName);
        mEmailTextView = (TextView) header.findViewById(R.id.email);
        mProfileImageView = (CircleImageView) header.findViewById(R.id.profileImage);
        sharedPrefManager = new SharedPrefManager(mContext);
        if (checkPlayServices()) {
            buildGoogleApiClient();
        }
        supportMapFragment =(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap);
        supportMapFragment.getMapAsync(this);
        Speedtextview=(TextView)findViewById(R.id.speedtextview);
        data = new Data(onGpsServiceUpdate);
        a=new Accelerationlist(getApplicationContext());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        onGpsServiceUpdate = new Data.onGpsServiceUpdate() {
            @Override
            public void update() {

            }
        };

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    protected void onPause() {
        supportMapFragment.onPause();
        super.onPause();
        mLocationManager.removeUpdates(this);
        mLocationManager.removeGpsStatusListener(this);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(data);
        prefsEditor.putString("data", json);
        prefsEditor.commit();
    }

    @Override
    public void onLowMemory() {
        supportMapFragment.onLowMemory();
        super.onLowMemory();
    }

    @Override
    protected void onStart() {
        supportMapFragment.onStart();
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        if(auth.getCurrentUser()!=null)
        {
            sharedPrefManager = new SharedPrefManager(mContext);
            String mUsername = sharedPrefManager.getName();
            String mEmail = sharedPrefManager.getUserEmail();
            mFullNameTextView.setText(mUsername);
            mEmailTextView.setText(mEmail);
            String uri = new String();
            uri =  sharedPrefManager.getPhoto();
            if(uri!=null)
            {
                if(!uri.equals("null"))
                {
                    Uri mPhotoUri = Uri.parse(uri);
                    Picasso.with(mContext).load(mPhotoUri).placeholder(android.R.drawable.sym_def_app_icon).error(android.R.drawable.sym_def_app_icon).into(mProfileImageView);
                }
                // showtoast("no photo");
                else
                {
                    mProfileImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showtoast("Upload Image");
                        }
                    });
                }
            }

        }
        else
        {
            startActivity(new Intent(NavActivity.this,LoginActivity.class));
            finish();
        }

    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        supportMapFragment.onResume();
        super.onResume();
        checkPlayServices();
        firstfix = true;
        if (!data.isRunning()){
            Gson gson = new Gson();
            String json = sharedPreferences.getString("data", "");
            data = gson.fromJson(json, Data.class);
        }
        if (data == null){
            data = new Data(onGpsServiceUpdate);
        }else{
            data.setOnGpsServiceUpdate(onGpsServiceUpdate);
        }

        if (mLocationManager.getAllProviders().indexOf(LocationManager.GPS_PROVIDER) >= 0) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 0, this);
        } else {
            Log.w("NavActivity", "No GPS location provider found. GPS data display will not be available.");
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDisabledDialog();
        }

        mLocationManager.addGpsStatusListener(this);
    }


    @SuppressLint("MissingPermission")
    private void displayLocation() {

        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();
            longi=""+longitude;
            lati=""+latitude;
            LatLng latLng = new LatLng(latitude, longitude);
            if (googleMap != null) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(20));
            }

        }
        }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void initFabbutton()
    {
        oneMoreFabMenu=(OneMoreFabMenu)findViewById(R.id.fab);
        oneMoreFabMenu.setOptionsClick(new OneMoreFabMenu.OptionsClick() {
            @Override
            public void onOptionClick(Integer integer) {
                    switch (integer) {
                        case R.id.option1:
                            Starttoggle();
                            break;
                        case R.id.option2:
                            Intent irp=new Intent(NavActivity.this, ReportProblemActivity.class);
                            irp.putExtra("longitude",longi);
                            irp.putExtra("latitude",lati);
                            startActivity(irp);
                            break;
                        case R.id.option3:
                            sharedPrefManager.saveLatitude(mContext,lati);
                            sharedPrefManager.saveLongitude(mContext,longi);
                            showtoast("Location saved");
                            break;
                        case R.id.option4:
                            Toast.makeText(NavActivity.this, "4", Toast.LENGTH_LONG).show();
                            break;
                    }
            }
        });

    }
    public void initNavigationDrawer(){

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                int id = item.getItemId();

                switch (id){
                    case R.id.Profile:
                        Toast.makeText(mContext,"!111",Toast.LENGTH_LONG).show();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.Contact:
                        Toast.makeText(mContext,"!222",Toast.LENGTH_LONG).show();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.Near_Hospital:
                        Intent irp1=new Intent(NavActivity.this, FileDownTryActivity.class);
                        startActivity(irp1);
                        Toast.makeText(mContext,"!333",Toast.LENGTH_LONG).show();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.highway_details:
                        Toast.makeText(mContext,"highway details",Toast.LENGTH_LONG).show();
                        break;
                    case R.id.report_problem:
                        Intent irp=new Intent(NavActivity.this, ReportProblemActivity.class);
                        irp.putExtra("longitude",longi);
                        irp.putExtra("latitude",lati);
                        startActivity(irp);
                        //Toast.makeText(mContext,"!444",Toast.LENGTH_LONG).show();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.track_problem:
                        Toast.makeText(mContext,"!555",Toast.LENGTH_LONG).show();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.drawer_item_settings:
                        Toast.makeText(getApplicationContext(),"Setting",Toast.LENGTH_SHORT).show();
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.logout:
                        navsignout();
                        Toast.makeText(getApplicationContext(),"Logout",Toast.LENGTH_SHORT).show();
                        drawerLayout.closeDrawers();
                        break;
                }
                return false;
            }
        });
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close){
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    public void Starttoggle()
    {
        if (!data.isRunning()) {
            data.setRunning(true);
            data.setFirstTime(true);
            showtoast("SmartDrive Safety service is ON");
            startService(new Intent(getBaseContext(), GpsServices.class));
        }else{
            data.setRunning(false);
            stopService(new Intent(getBaseContext(), GpsServices.class));
            showtoast("SmartDrive Safety service is OFF");
        }
    }
    @Override
    public void onLocationChanged(Location location) {

        if (location.hasAccuracy()) {
            SpannableString s = new SpannableString(String.format("%.0f", location.getAccuracy()) + "m");
            s.setSpan(new RelativeSizeSpan(0.75f), s.length() - 1, s.length(), 0);
            if (firstfix) {
                firstfix = false;
            }
        } else {
            firstfix = true;
        }
        if (location.hasSpeed()) {
            String speed = String.format(Locale.ENGLISH, "%.0f", location.getSpeed() * 3.6) + "km/h";
            SpannableString s = new SpannableString(speed);
            s.setSpan(new RelativeSizeSpan(0.25f), s.length() - 4, s.length(), 0);

            //getacceleration(location.getSpeed());
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            longi=""+longitude;
            lati=""+latitude;
            LatLng latLng = new LatLng(latitude, longitude);
            if(data.isRunning())
            {
                Speedtextview.setText(s);
                if (googleMap != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(20));
                }
            }

        }
    }
//
//    public void getacceleration(double l)
//    {
//        double speeddiff=l-oldspeed;
//        double timediff=(System.currentTimeMillis()-oldtime)/(1000);
//        oldtime=System.currentTimeMillis();
//        double acc=speeddiff/timediff;
//        a.adddata(oldspeed,l,acc);
//        //writetofile(datatosave);
//        oldspeed=l;
//        if(a.ismessagechange())
//        {
//            String title = a.getMessage();
//            if(title.equals("1"))
//            {
//                showMessageDialog("Accident is detected");
//            }
////            if(title.equals("0"))
////            {
////                showMessageDialog("False Alert");
////            }
//
//        }
//    }
    public void showMessageDialog(String amessage){
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this)
                        .setTitle("Auto Alert System")
                        .setMessage(amessage)
                        .setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(NavActivity.this,"Accident accepted",Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

// Show the AlertDialog.
        AlertDialog alertDialog = alertDialogBuilder.show();
    }

    public void showtoast(String s)
    {
        LayoutInflater li = getLayoutInflater();
        View layout = li.inflate(R.layout.custom_toast,(ViewGroup) findViewById(R.id.toast_root));
        TextView text = (TextView) layout.findViewById(R.id.toast_error);
        text.setText(s);
        Toast toast = new Toast(getApplicationContext());// Get Toast Context
        toast.setGravity(Gravity.BOTTOM| Gravity.FILL_HORIZONTAL, 0, 0);// Set
        toast.setDuration(Toast.LENGTH_SHORT);// Set Duration
        toast.setView(layout); // Set Custom View over toast
        toast.show();// Finally show toast
    }
//    void trt()
// {
//    double latitude = location.getLatitude();
//    double longitude = location.getLongitude();
//    LatLng latLng = new LatLng(latitude, longitude);
//    MarkerOptions userIndicator = new MarkerOptions()
//            .position(latLng)
//            .title("You are here")
//            .snippet("lat:" + latitude + ", lng:" + longitude)
//            .title("Here");
////        Circle circle =  googleMap.addCircle(new CircleOptions()
////                .center(latLng)
////                .radius(1000)
////                .strokeColor(Color.RED)
////                .fillColor(Color.BLUE));
//        if(googleMap != null)
//    {
//        googleMap.clear();
//        googleMap.addMarker(userIndicator);
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//        googleMap.animateCamera(CameraUpdateFactory.zoomTo(20));
//        googleMap.addCircle(new CircleOptions()
//                .center(latLng)
//                .radius(5)
//                .strokeWidth(0f)
//                .fillColor(0x550000FF));
//    }
// }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap1) {
        googleMap = googleMap1;
        googleMap.setMyLocationEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        displayLocation();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                @SuppressLint("MissingPermission") GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
                int satsInView = 0;
                int satsUsed = 0;
                Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
                for (GpsSatellite sat : sats) {
                    satsInView++;
                    if (sat.usedInFix()) {
                        satsUsed++;
                    }
                }
                if (satsUsed == 0) {
                    data.setRunning(false);
                    stopService(new Intent(getBaseContext(), GpsServices.class));
                    firstfix = true;
                }
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showGpsDisabledDialog();
                }
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;
        }

    }
    public void showGpsDisabledDialog(){
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this)
                        .setTitle("GPS is Settings")
                        .setMessage("GPS is not Enabled. Do you want to go to settings menu?")
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                mContext.startActivities(new Intent[]{intent});
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

// Show the AlertDialog.
        AlertDialog alertDialog = alertDialogBuilder.show();
    }
    public static Data getData() {
        return data;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    private void navsignout()
    {
        new SharedPrefManager(mContext).clear();
        auth.signOut();
        Intent intent = new Intent(NavActivity.this, FActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
