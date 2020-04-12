package com.quantumcoders.travelpool;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.quantumcoders.travelpool.utility.AppConstants;
import com.quantumcoders.travelpool.utility.RideInfo;
import com.quantumcoders.travelpool.utility.Util;

public class RideBookingActivity extends AppCompatActivity {
    MapView map = null;
    GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_booking);

        RideInfo rideInfo = (RideInfo) getIntent().getSerializableExtra("RideInfo");

        //init views
        ((TextView)findViewById(R.id.tv_driverName)).setText(rideInfo.getDriverName());
        ((TextView)findViewById(R.id.tv_date)).setText(rideInfo.getDate());
        ((TextView)findViewById(R.id.tv_time)).setText(rideInfo.getTime());
        ((TextView)findViewById(R.id.tv_vehicle)).setText(rideInfo.getVehicleDetails());
        ((TextView)findViewById(R.id.tv_extra)).setText(rideInfo.getExtraDetails());
        ((TextView)findViewById(R.id.tv_phone)).setText(rideInfo.getDriverPhone());
        ((TextView)findViewById(R.id.tv_rideStatus)).setText(rideInfo.getRideStatus());

        //init map
        map = findViewById(R.id.map);
        map.onCreate(new Bundle());
        map.getMapAsync((gmap)->{
            googleMap = gmap;
            map.onResume();
            LatLng source = new LatLng(rideInfo.sourceLat,rideInfo.sourceLng);
            LatLng dest = new LatLng(rideInfo.destLat,rideInfo.destLng);
            map.post(()->{
                gmap.addMarker(new MarkerOptions().title("Source").position(source));
                gmap.addMarker(new MarkerOptions().title("Destination").position(dest));
                gmap.setMyLocationEnabled(true);
                gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().include(source).include(dest).build(),10));
                gmap.addPolyline(new PolylineOptions().add(source,dest).color(Color.BLUE));
            });
        });


        //init bookRide button
        final String email = Util.encodeEmail(Util.sprefGetString(this, AppConstants.EMAIL_KEY));
        final String phone = Util.sprefGetString(this,AppConstants.PHONE_KEY);
        findViewById(R.id.btnBookRide).setOnClickListener((v)->{

            EditText nameText = new EditText(this);
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            param.setMarginStart(Util.dpToPx(this,15));
            param.setMarginEnd(Util.dpToPx(this,15));
            nameText.setLayoutParams(param);
            nameText.setHint("Your Name");

            //show dialog box to ask for name
            new AlertDialog.Builder(this).setTitle("Enter your name : ").setView(nameText)
                    .setPositiveButton("Book Ride",(dialogInterface, i) -> {
                        String name = nameText.getText().toString();
                        if(name.trim().isEmpty()){
                            Util.shortToast(RideBookingActivity.this,"Please enter name");
                            dialogInterface.cancel();
                            return;
                        }
                        //else

                        //set user's booking status to the ride id
                        // set ride booking status to booked
                        //set rideinfo booker details  - email, phone, name
                        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(email).child(AppConstants.DBKEY_USER_BOOKED_RIDE)
                                .setValue(rideInfo.getRideID())
                        .addOnSuccessListener(vd->{
                            rideInfo.setBookingStatus(AppConstants.BOOKED);
                            rideInfo.setBookerEmail(email);
                            rideInfo.setBookerPhone(phone);
                            rideInfo.setBookerName(nameText.getText().toString());
                            FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(rideInfo.getRideID())
                                    .setValue(rideInfo).addOnSuccessListener(vd2 -> {
                                RideBookingActivity.this.setResult(Activity.RESULT_OK);
                                RideBookingActivity.this.finish();
                                //now the ride is booked
                            }).addOnFailureListener(ex ->{
                                Util.shortToast(RideBookingActivity.this,"Error occured " + ex.getMessage());
                            });
                        }).addOnFailureListener(ex ->{
                            Util.shortToast(RideBookingActivity.this,"Error occured " + ex.getMessage());
                        });;

                    })
                    .setNegativeButton("Cancel",(dialogInterface, i) -> {
                        dialogInterface.cancel();
                    }).create().show();
        });

    }
}
