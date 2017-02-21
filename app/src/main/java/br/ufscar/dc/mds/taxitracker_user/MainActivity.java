package br.ufscar.dc.mds.taxitracker_user;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;

import org.json.JSONObject;

import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestClientUsage;
import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestHandler;

public class MainActivity extends AppCompatActivity implements TaxiTrackerRestHandler {
    private static final String TAG = "MainActivity";
    TaxiTrackerRestClientUsage taxiTrackerRest;
    private final static String AUTH_USER_TAG = "PASSAGEIRO";
    private static final int RC_SIGN_IN = 0;
    private FirebaseAuth auth;

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
            completeLogin();
        } else {
            startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setProviders(
                            AuthUI.GOOGLE_PROVIDER)
                    .build(), RC_SIGN_IN);
        }
    }

    private void completeLogin() {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            mProgress.setVisibility(View.VISIBLE);
            if (resultCode == RESULT_OK) {
                //user logged in
                completeLogin();
            } else {
                //user not authenticated
                Log.d("AUTH", "NOT AUTHENTICATED");
            }
        }
    }

    @Override
    public void on_start_login() {
        //Toast.makeText(this, "starting loging", Toast.LENGTH_LONG).show();
    }

    @Override
    public void on_login(String access_token) {
        Log.d(TAG, "logged with token " + access_token);
        taxiTrackerRest.add_auth_token(access_token);
    }

    @Override
    public void on_refresh_info(JSONObject response) {

    }

    @Override
    public void on_race_created(JSONObject response) {

    }
}