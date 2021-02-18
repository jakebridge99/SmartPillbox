package com.example.smartpillbox;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class UserHomeScreen extends AppCompatActivity {

    GoogleSignInClient mGoogleSignInClient;
    Button sign_out;

    //User details
    //These values are taken from the users Google account
    TextView nameTV;    //Users name
    TextView emailTV;   //Users email
    TextView idTV;      //Users id number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);     //sIS loads the users data from last app use
        setContentView(R.layout.burger_menu);   //Add burger menu to home page (see burger_menu.xml for info)

        //Adds toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        sign_out = findViewById(R.id.log_out);
        nameTV = findViewById(R.id.name);
        emailTV = findViewById(R.id.email);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(UserHomeScreen.this);
        if (acct != null) {
            // Get values from Google account
            String personName = acct.getDisplayName();
            String personGivenName = acct.getGivenName();
            String personFamilyName = acct.getFamilyName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();

            nameTV.setText("Name: "+personName);
            emailTV.setText("Email: "+personEmail);
        }

        //Sign out button listener
        sign_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.nav_home:
//                // User chose the "Settings" item, show the app settings UI...
//                return true;
//
//            case R.id.nav_schedule:
//                // User chose the "Favorite" action, mark the current item
//                // as a favorite...
//                return true;
//            case R.id.nav_medications:
//                // User chose the "Favorite" action, mark the current item
//                // as a favorite...
//                return true;
//
//            case R.id.nav_account:
//                // User chose the "Favorite" action, mark the current item
//                // as a favorite...
//                return true;


            case R.id.nav_logout:
                signOut();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    //Sign out of Google account/application
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(UserHomeScreen.this,"Successfully signed out", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(UserHomeScreen.this, GoogleSignIn.class));
                        finish();
                    }
                });
    }
}