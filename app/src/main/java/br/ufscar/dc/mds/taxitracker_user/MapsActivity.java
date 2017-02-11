package br.ufscar.dc.mds.taxitracker_user;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionApi;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.UnsupportedEncodingException;

import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestClientUsage;
import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestHandler;

public class MapsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TaxiTrackerRestHandler,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

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

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppIndex.API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient.connect();
        AppIndex.AppIndexApi.start(mGoogleApiClient, getIndexApiAction());
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
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
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        Log.i(TAG, "got mLastLocation " + mLastLocation);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Extracts data from PlacePicker result.
     * This method is called when an Intent has been started by calling
     * {@link #startActivityForResult(android.content.Intent, int)}. The Intent for the
     * {@link com.google.android.gms.location.places.ui.PlacePicker} is started with
     * {@link #REQUEST_PLACE_PICKER} request code. When a result with this request code is received
     * in this method, its data is extracted by converting the Intent data to a {@link Place}
     * through the
     * {@link com.google.android.gms.location.places.ui.PlacePicker#getPlace(android.content.Intent,
     * android.content.Context)} call.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // BEGIN_INCLUDE(activity_result)
        if (requestCode == REQUEST_PLACE_PICKER) {
            // This result is from the PlacePicker dialog.

            // Enable the picker option
            //showPickAction(true);

            if (resultCode == Activity.RESULT_OK) {
                /* User has picked a place, extract data.
                   Data is extracted from the returned intent by retrieving a Place object from
                   the PlacePicker.
                 */
                final Place place = PlacePicker.getPlace(data, MapsActivity.this);

                /* A Place object contains details about that place, such as its name, address
                and phone number. Extract the name, address, phone number, place ID and place types.
                 */
                final CharSequence name = place.getName();
                final CharSequence address = place.getAddress();
                final CharSequence phone = place.getPhoneNumber();
                final String placeId = place.getId();
                String attribution = PlacePicker.getAttributions(data);
                if (attribution == null) {
                    attribution = "";
                }

                // Print data to debug log
                Log.d(TAG, "Place selected: " + placeId + " (" + name.toString() + ")");
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
             /*   if (mGoogleApiClient == null) Log.e(TAG, "mGoogleApiClient is null");
                PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                        .getCurrentPlace(mGoogleApiClient, null);
*/
                try {

                    Log.i(TAG, mLastLocation.toString());
                    Log.i(TAG, place.getLatLng().toString());
                    Log.d(TAG, "creating race");
                    taxiTrackerRest.createRace(MapsActivity.this, mLastLocation.toString(), place.getLatLng().toString());
                } catch (UnsupportedEncodingException | NullPointerException e) {
                    e.printStackTrace();
                }
/*
                result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                    @Override
                    public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                        try {
                            Log.d(TAG, "creating race");
                            if (likelyPlaces.getCount() <= 0) {
                                Log.e(TAG, "likelyPlaces é " + likelyPlaces.getCount());
                            } else {
                                taxiTrackerRest.createRace(MapsActivity.this, likelyPlaces.get(0).getPlace().getLatLng().toString(), place.getLatLng().toString());
                                Log.i(TAG, likelyPlaces.get(0).getPlace().getLatLng().toString());
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, place.getLatLng().toString());

                        for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                            Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                                    placeLikelihood.getPlace().getName(),
                                    placeLikelihood.getLikelihood()));
                        }
                        likelyPlaces.release();
                    }
                });
*/
                // Show the info
                Toast.makeText(this, getString(R.string.detail_text, placeId, address, phone,
                        attribution),
                        Toast.LENGTH_LONG)
                        .show();
            } else {
                // User has not selected a place, hide the card.
                Toast.makeText(this, "did not select a place",
                        Toast.LENGTH_LONG)
                        .show();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
        // END_INCLUDE(activity_result)
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "onConnectionFailed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null)
            mLastLocation = location;
        Log.i(TAG, "got mLastLocation " + mLastLocation);
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000);
        mLocationRequest.setFastestInterval(30000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return mLocationRequest;
    }

}
