package com.example.smartpillbox;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

public class UserHomeScreen extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawer;
    GoogleSignInClient mGoogleSignInClient;
    //User details
    //These values are taken from the users Google account
    TextView nameHeader;    //Users name
    TextView emailHeader;   //Users email

    /*
    onCreate makes the burger menu for the app.
    @param savedInstance :
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);     //sIS loads the users data from last app use
        setContentView(R.layout.burger_menu);   //Add burger menu to home page (see burger_menu.xml for info)
        createNotificationChannel(); //Create our notification channel
        //Adds toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        mDrawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View hView = navigationView.getHeaderView(0);
        TextView nameHeader = (TextView) hView.findViewById(R.id.name_header);
        TextView emailHeader = (TextView) hView.findViewById(R.id.email_header);
        ImageView user_profile = (ImageView) hView.findViewById(R.id.user_pfp);



        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, myToolbar,R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new AccountFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

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

            nameHeader.setText(personName);
            emailHeader.setText(personEmail);
            Picasso.get().load(acct.getPhotoUrl()).into(user_profile);
        }
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "MedicationReminderChannel";
            String description = "Channel for Medication Reminder";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("channel1", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    /*
    onBackPressed brings the user to the previous page they were on.
     */
    @Override
    public void onBackPressed(){
        if(mDrawer.isDrawerOpen(GravityCompat.START)){
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    /*
    onNavigationItemSelected is used to switch between the pages of the app in the burger menu.
    @param item : the selected page to go to
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.nav_account:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new AccountFragment()).commit();
                break;
            case R.id.nav_medications:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new MedicationFragment()).commit();
                break;
            case R.id.nav_schedule:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new ScheduleFragment()).commit();
                break;
            case R.id.nav_logout:
                signOut();
                break;
        }
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /*
    signOut signs user out of Google account/application
     */
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(UserHomeScreen.this,"Successfully signed out", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(UserHomeScreen.this, GoogleSignIn.class));
                        finish();
                    }
                });
    }
}
