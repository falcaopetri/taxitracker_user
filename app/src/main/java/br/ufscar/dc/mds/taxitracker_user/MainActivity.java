package br.ufscar.dc.mds.taxitracker_user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.io.UnsupportedEncodingException;

import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestClientUsage;
import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestHandler;

public class MainActivity extends AppCompatActivity implements TaxiTrackerRestHandler {
    TaxiTrackerRestClientUsage taxiTrackerRest;
    private final static String AUTH_USER_TAG = "passageiro";
    private static final int RC_SIGN_IN = 0;
    private FirebaseAuth auth;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private FirebaseUser mUser;
    private String idToken;
    private ProgressBar mProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgress = (ProgressBar) findViewById(R.id.progressBar);

        taxiTrackerRest = new TaxiTrackerRestClientUsage(this);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            //user already signed in
            Log.d("AUTH", auth.getCurrentUser().getEmail());
            mProgress.setVisibility(View.VISIBLE);
            auth.getCurrentUser().getToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                public void onComplete(@NonNull Task<GetTokenResult> task) {
                    mProgress.setVisibility(View.INVISIBLE);
                    if (task.isSuccessful()) {
                        idToken = task.getResult().getToken();
                        Log.d("ID TOKEN", idToken);
                        // Send token to your backend via HTTPS
                        taxiTrackerRest.login(idToken, AUTH_USER_TAG);
                        // ...
                        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                        startActivity(intent);
                        MainActivity.this.finish();
                    } else {
                        // Handle error -> task.getException();
                    }
                }
            });
        } else {
            startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setProviders(
                            AuthUI.GOOGLE_PROVIDER)
                    .build(), RC_SIGN_IN);
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            mProgress.setVisibility(View.VISIBLE);
            if (resultCode == RESULT_OK) {
                //user logged in
                Log.d("AUTH", auth.getCurrentUser().getUid());
                mUser = auth.getCurrentUser();
                mUser.getToken(true)
                        .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                            public void onComplete(@NonNull Task<GetTokenResult> task) {
                                mProgress.setVisibility(View.INVISIBLE);
                                if (task.isSuccessful()) {
                                    idToken = task.getResult().getToken();
                                    Log.d("ID TOKEN", idToken);
                                    // Send token to your backend via HTTPS
                                    taxiTrackerRest.login(idToken, AUTH_USER_TAG);
                                    // ...
                                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                                    startActivity(intent);
                                    MainActivity.this.finish();
                                } else {
                                    // Handle error -> task.getException();
                                }
                            }
                        });


            } else {
                //user not authnticated
                Log.d("AUTH", "NOT AUTHENTICATED");
            }
        }

    }


    @Override
    public void on_start_login() {
        Toast.makeText(this, "starting loging", Toast.LENGTH_LONG).show();
    }

    @Override
    public void on_login(String access_token) {
        Toast.makeText(this, "logged with token " + access_token, Toast.LENGTH_LONG).show();
        taxiTrackerRest.add_auth_token(access_token);

        //taxiTrackerRest.getPassageiros();

        /*try {
            taxiTrackerRest.createRace(this, "origem", "destino");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/
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
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}