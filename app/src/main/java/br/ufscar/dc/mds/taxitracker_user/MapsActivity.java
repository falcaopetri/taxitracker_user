package br.ufscar.dc.mds.taxitracker_user;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestClientUsage;
import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestHandler;

public class MapsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TaxiTrackerRestHandler,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, OnMapReadyCallback {

    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting";
    private static final String LOCATION_KEY = "location";
    GoogleMap map;

    private static final int RC_SIGN_IN = 0;

    private static final String TAG = "MapsActivity";

    TaxiTrackerRestClientUsage taxiTrackerRest;
    LocationRequest locationRequest = createLocationRequest();

    /**
     * Action to launch the PlacePicker from a card. Identifies the card action.
     */
    private static final int ACTION_PICK_PLACE = 1;

    /**
     * Request code passed to the PlacePicker intent to identify its result when it returns.
     */
    private static final int REQUEST_PLACE_PICKER = 1;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    final Handler handler = new Handler();
    private Timer timer = new Timer();
    private Marker motoristaMarker;
    private boolean mRequestingLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        taxiTrackerRest = new TaxiTrackerRestClientUsage(this);
        taxiTrackerRest.getPassageiros();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Iniciando uma corrida.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // BEGIN_INCLUDE(intent)
            /* Use the PlacePicker Builder to construct an Intent.
            Note: This sample demonstrates a basic use case.
            The PlacePicker Builder supports additional properties such as search bounds.
             */
                try {
                    PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                    Intent intent = intentBuilder.build(MapsActivity.this);
                    // Start the Intent by requesting a result, identified by a request code.
                    startActivityForResult(intent, REQUEST_PLACE_PICKER);

                    // Hide the pick option in the UI to prevent users from starting the picker
                    // multiple times.
                    //showPickAction(false);

                } catch (GooglePlayServicesRepairableException e) {
                    GooglePlayServicesUtil
                            .getErrorDialog(e.getConnectionStatusCode(), MapsActivity.this, 0);
                } catch (GooglePlayServicesNotAvailableException e) {
                    Toast.makeText(MapsActivity.this, "Google Play Services is not available.",
                            Toast.LENGTH_LONG)
                            .show();
                }

                // END_INCLUDE(intent)
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        updateValuesFromBundle(savedInstanceState);
    }


    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                if (mLastLocation != null)
                    mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
        }
    }

    public void refreshInfoInstall() {
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        refreshInfo();
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 10000); //execute in every 50000 ms
    }

    protected void refreshInfo() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            return;
        }

        Log.i(TAG, "updating passageiro info");
        taxiTrackerRest.refreshPassageiroInfo();

        return;
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e(TAG, "permission check failed");
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation == null) {
            mLastLocation = new Location("");
            mLastLocation.setLatitude(-21.979803);
            mLastLocation.setLongitude(-47.881376);
        }
        LatLng p = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(p, 15));
        Log.i(TAG, "got mLastLocation " + mLastLocation);
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // BEGIN_INCLUDE(activity_result)
        if (requestCode == REQUEST_PLACE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                /* User has picked a place, extract data.
                   Data is extracted from the returned intent by retrieving a Place object from
                   the PlacePicker.
                 */
                final Place place = PlacePicker.getPlace(data, MapsActivity.this);

                final CharSequence name = place.getName();
                final CharSequence address = place.getAddress();
                final CharSequence phone = place.getPhoneNumber();
                final String placeId = place.getId();
                String attribution = PlacePicker.getAttributions(data);
                if (attribution == null) {
                    attribution = "";
                }

                Log.d(TAG, "Place selected: " + placeId + " (" + name.toString() + ")");

                try {
                    Log.d(TAG, "creating race");
                    Log.i(TAG, mLastLocation.toString());
                    Log.i(TAG, place.getLatLng().toString());
                    String s = place.getLatLng().latitude + "," + place.getLatLng().longitude;
                    String s2 = mLastLocation.getLatitude() + "," + mLastLocation.getLongitude();
                    taxiTrackerRest.createRace(MapsActivity.this, s2, s);
                } catch (UnsupportedEncodingException | NullPointerException e) {
                    e.printStackTrace();
                }
                // Show the info
                /*Toast.makeText(this, getString(R.string.detail_text, placeId, address, phone,
                        attribution),
                        Toast.LENGTH_LONG)
                        .show();*/
            } else {
                // User has not selected a place, hide the card.
                Toast.makeText(this, "did not select a place",
                        Toast.LENGTH_LONG)
                        .show();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, locationRequest, this);
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation == null) {
                mLastLocation = new Location("");
                mLastLocation.setLatitude(-21.979803);
                mLastLocation.setLongitude(-47.881376);
            }
            LatLng p = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(p, 15));
            Log.i(TAG, "got mLastLocation " + mLastLocation);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_logout) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d("AUTH", "USER LOGGED OUT");
                            //finish();

                            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                            startActivity(intent);
                            MapsActivity.this.finish();
                        }
                    });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void on_start_login() {
        throw new RuntimeException("should already be logged in");
    }

    @Override
    public void on_login(String access_token) {
        throw new RuntimeException("should already be logged in");
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mLastLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void on_refresh_info(JSONObject response) {
        try {
            String pos[] = response.getString("motorista_location").split(",");
            LatLng p = new LatLng(Double.parseDouble(pos[0]), Double.parseDouble(pos[1]));
            if (motoristaMarker == null)
                motoristaMarker = map.addMarker(new MarkerOptions()
                        .position(p));
            else {
                motoristaMarker.setPosition(p);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(p, 15));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            motoristaMarker.remove();
        }
    }

    @Override
    public void on_race_created(JSONObject response) {
        refreshInfoInstall();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "onConnectionFailed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mLastLocation = location;
        }
        if (mLastLocation != null) {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
        Log.i(TAG, "got mLastLocation " + mLastLocation);
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120);
        mLocationRequest.setFastestInterval(300);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        this.map.setMyLocationEnabled(true);
        this.map.getUiSettings().setMyLocationButtonEnabled(true);
        this.map.getUiSettings().setCompassEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        map.animateCamera(CameraUpdateFactory.zoomIn());
        Log.i(TAG, "got mLastLocation " + mLastLocation);
    }
}
