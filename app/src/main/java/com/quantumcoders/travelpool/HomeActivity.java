package com.quantumcoders.travelpool;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.quantumcoders.travelpool.fragments.MyBookedRideFragment;
import com.quantumcoders.travelpool.fragments.MyOfferedRideFragment;
import com.quantumcoders.travelpool.fragments.OfferRideFragment;
import com.quantumcoders.travelpool.fragments.TakeRideFragment;
import com.quantumcoders.travelpool.utility.AppConstants;
import com.quantumcoders.travelpool.utility.Util;

import java.util.function.BiConsumer;

import static com.quantumcoders.travelpool.utility.AppConstants.REQCODE_BOOK_RIDE;

public class HomeActivity extends AppCompatActivity {

    public double curLat,curLng;
    DrawerLayout drawerLayout = null;
    Toolbar toolbar = null;
    ActionBarDrawerToggle toggle = null;
    NavigationView drawerView = null;
    String email;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawer);
        drawerView = findViewById(R.id.navigation_view);

        setLocationListener();

        email = Util.sprefGetString(this,AppConstants.EMAIL_KEY);
        email = Util.encodeEmail(email);

        //set toolbar to use toggle button for drawer
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //set drawer toggle button
        toggle = new ActionBarDrawerToggle(this, drawerLayout,toolbar,R.string.str_open_drawer,R.string.str_close_drawer);
        drawerLayout.addDrawerListener(toggle);
        drawerLayout.post(()->{
            toggle.syncState();
            goToFragment(R.id.takeRideOption);
        });

        //set navigation view
        drawerView.setNavigationItemSelectedListener(this::navDrawerItemSelected);

        Util.shortToast(this, Util.sprefGetString(this, AppConstants.PHONE_KEY));
    }

    public void goToFragment(int id){
        navDrawerItemSelected(drawerView.getMenu().findItem(id));
        drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public boolean navDrawerItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.takeRideOption:{
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new TakeRideFragment(),"takeRideFragment")
                        .commit();
                break;
            }
            case R.id.offerRideOption:{
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new OfferRideFragment(),"offerRideFragment")
                        .commit();
                break;
            }
            case R.id.myBookedRidesOption:{
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new MyBookedRideFragment(),"myBookedRideFragment")
                        .commit();
                break;
            }
            case R.id.myOfferedRidesOption:{
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new MyOfferedRideFragment(),"myOfferedRideFragment")
                        .commit();
                break;
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    volatile boolean inRide = false;
    volatile boolean isDriving = false; //is driver or passenger
    String rideID = "";

    public void userInRide(boolean isDriving,String ride_id){
        if(!inRide){
            inRide = true;
            this.isDriving = isDriving;
            rideID = ride_id;
            if(!isDriving){
                //if user is not driving i.e. he is taking ride so when the ride ends the function userNotInRide
                // should be called so that further location updates will not be writtent to firebase.
                // It isn't required in driver's case because when driver ends the ride, the function will be
                // called when the end ride button is clicked.
                FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride_id)
                        .child(AppConstants.DBKEY_RIDE_STATUS).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String rideStatus = dataSnapshot.getValue(String.class);
                        if(rideStatus.equals(AppConstants.RIDE_ENDED)){
                            Util.shortToast(HomeActivity.this,"Ride has ended");
                            userNotInRide();
                            dataSnapshot.getRef().removeEventListener(this);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            }
        }
    }

    public void userNotInRide(){
        inRide=false;
        isDriving=false;
        rideID="";
        Util.shortToast(this,"Ride has ended");
        goToFragment(R.id.takeRideOption);
    }

    @SuppressLint("MissingPermission")
    public void setLocationListener(){
        LocationManager locman = (LocationManager) getSystemService(LOCATION_SERVICE);
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                curLat = location.getLatitude();
                curLng = location.getLongitude();
                Log.d("Location","Lat = " + curLat + " | Lng = " + curLng);

                if(inRide){
                    // code to update location on firebase acc to the ride id
                    if(isDriving){  //update driver latlng
                        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(rideID)
                                .child(AppConstants.DBKEY_DRIVER_LAT).setValue(curLat);
                        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(rideID)
                                .child(AppConstants.DBKEY_DRIVER_LNG).setValue(curLng);
                    } else {    //update booker latlng
                        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(rideID)
                                .child(AppConstants.DBKEY_BOOKER_LAT).setValue(curLat);
                        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(rideID)
                                .child(AppConstants.DBKEY_BOOKER_LNG).setValue(curLng);
                    }
                }
            }

            @Override public void onStatusChanged(String s, int i, Bundle bundle) { }
            @Override public void onProviderEnabled(String s) { }
            @Override public void onProviderDisabled(String s) { }
        };

        if(locman.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 5, listener);
        } else if(locman.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            locman.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,3000,5,listener);
        }
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        } else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == REQCODE_BOOK_RIDE) {
            if(resultCode==RESULT_OK){
                ((HomeActivity)this).goToFragment(R.id.takeRideOption);
                Util.shortToast(this,"Ride Booked Successfully");
            }
        }
    }
}
