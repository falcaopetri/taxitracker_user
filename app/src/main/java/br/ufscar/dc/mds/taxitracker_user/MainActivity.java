package br.ufscar.dc.mds.taxitracker_user;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.io.UnsupportedEncodingException;

import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestClientUsage;
import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestHandler;

public class MainActivity extends AppCompatActivity implements TaxiTrackerRestHandler, View.OnClickListener{
    TaxiTrackerRestClientUsage taxiTrackerRest;
    private final static String AUTH_USER_TAG = "passageiro";
    private static final int RC_SIGN_IN = 0;
    private FirebaseAuth auth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taxiTrackerRest = new TaxiTrackerRestClientUsage(this);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null){
            //user already signed in
            Log.d("AUTH", auth.getCurrentUser().getEmail());
        } else {
            startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setProviders(
                            AuthUI.GOOGLE_PROVIDER)
                    .build(), RC_SIGN_IN);
        }

        findViewById(R.id.log_out_button).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            if (resultCode == RESULT_OK){
                //user logged in
                Log.d("AUTH", auth.getCurrentUser().getEmail());
            } else {
                //user not authnticated
                Log.d("AUTH", "NOT AUTHENTICATED");
            }
        }
    }

    public void login(View view) {
        // método chamando como callback de um botão

        // generate the token @ http://stackoverflow.com/a/27904796
        // TODO esse id_token é o retornado pela API do google Oauth 2.0 após a autenticação
        // Dica: É uma string bem longa...
        String id_token = "";
        taxiTrackerRest.login(id_token, AUTH_USER_TAG);
    }

    @Override
    public void on_start_login() {
        Toast.makeText(this, "starting loging", Toast.LENGTH_LONG).show();
    }

    @Override
    public void on_login(String access_token) {
        Toast.makeText(this, "logged with token " + access_token, Toast.LENGTH_LONG).show();
        taxiTrackerRest.add_auth_token(access_token);

        taxiTrackerRest.getPassageiros();

        try {
            taxiTrackerRest.createRace(this, "origem", "destino");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.log_out_button){
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d("AUTH", "USER LOGGED OUT");
                            finish();
                        }
                    }  );
        }
    }
}