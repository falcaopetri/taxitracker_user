package br.ufscar.dc.mds.taxitracker_user;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestClientUsage;
import br.ufscar.dc.mds.taxitracker_library.TaxiTrackerRestHandler;

public class MainActivity extends AppCompatActivity implements TaxiTrackerRestHandler {
    TaxiTrackerRestClientUsage taxiTrackerRest;
    private final static String AUTH_USER_TAG = "passageiro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taxiTrackerRest = new TaxiTrackerRestClientUsage(this);
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
}