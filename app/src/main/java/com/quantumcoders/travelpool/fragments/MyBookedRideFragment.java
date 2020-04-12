package com.quantumcoders.travelpool.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.quantumcoders.travelpool.HomeActivity;
import com.quantumcoders.travelpool.R;
import com.quantumcoders.travelpool.utility.AppConstants;
import com.quantumcoders.travelpool.utility.RideInfo;
import com.quantumcoders.travelpool.utility.Util;

public class MyBookedRideFragment extends Fragment {

    View fragmentView;
    TextView tv_driverName, tv_date, tv_time, tv_vehicle, tv_extra, tv_phone, tv_rideStatus;
    TextView tv_bookingStatus, tv_bookerName, tv_bookerPhone;
    Button btnStartEndRide;
    MapView map;
    GoogleMap googleMap;
    LatLng source, dest;
    RideInfo ride;

    String email;


    double driverLat,driverLng;
    Marker driver;
    DatabaseReference driverLatRef,driverLngRef;
    ValueEventListener driverLatListener,driverLngListener;
    Object lock = new Object();

    ValueEventListener rideStartListener = null;
    DatabaseReference rideStartRef = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_my_booked_ride, container, false);

        tv_driverName = fragmentView.findViewById(R.id.tv_driverName);
        tv_date = fragmentView.findViewById(R.id.tv_date);
        tv_time = fragmentView.findViewById(R.id.tv_time);
        tv_vehicle = fragmentView.findViewById(R.id.tv_vehicle);
        tv_extra = fragmentView.findViewById(R.id.tv_extra);
        tv_phone = fragmentView.findViewById(R.id.tv_phone);
        tv_rideStatus = fragmentView.findViewById(R.id.tv_rideStatus);
        tv_bookingStatus = fragmentView.findViewById(R.id.tv_bookingStatus);
        tv_bookerName = fragmentView.findViewById(R.id.tv_bookerName);
        tv_bookerPhone = fragmentView.findViewById(R.id.tv_bookerPhone);
        btnStartEndRide = fragmentView.findViewById(R.id.btnStartEndRide);
        map = fragmentView.findViewById(R.id.map);
        email = Util.encodeEmail(Util.sprefGetString(getActivity(), AppConstants.EMAIL_KEY));

        //get ride ID
        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(email)
                .child(AppConstants.DBKEY_USER_BOOKED_RIDE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String value = dataSnapshot.getValue(String.class);
                        System.out.println("Obtained Ride ID  = " + value);
                        onObtainedRideID(value);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { someErrorOccured(); }
                });


        return fragmentView;
    }

    public void onObtainedRideID(String rideId){
        if(rideId.equals("0")){
            //if not booked any ride, show message
            fragmentView.findViewById(R.id.bookedMessage).setVisibility(View.VISIBLE);
        } else {
            //else get ride info and do everything else
            //this also means that the ride booking status is BOOKED
            FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(rideId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            ride = dataSnapshot.getValue(RideInfo.class);
                            initViewsWithRideInfo(ride);
                            if(ride.getRideStatus().equals(AppConstants.RIDE_STARTED)){
                                rideHasStarted();
                            } else {
                                rideHasNotStarted();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            someErrorOccured();
                        }
                    });
        }
    }

    public void initViewsWithRideInfo(RideInfo rideInfo){
        tv_driverName.setText(rideInfo.getDriverName());
        tv_date.setText(rideInfo.getDate());
        tv_time.setText(rideInfo.getTime());
        tv_vehicle.setText(rideInfo.getVehicleDetails());
        tv_extra.setText(rideInfo.getExtraDetails());
        tv_phone.setText(rideInfo.getDriverPhone());
        tv_rideStatus.setText(rideInfo.getRideStatus());
        tv_bookingStatus.setText(rideInfo.getBookingStatus());
        tv_bookerName.setText(rideInfo.getBookerName());
        tv_bookerPhone.setText(rideInfo.getBookerPhone());
        source = new LatLng(rideInfo.getSourceLat(), rideInfo.getSourceLng());
        dest = new LatLng(rideInfo.getDestLat(), rideInfo.getDestLng());
        ride = rideInfo;

        //setup the map
        map.onCreate(new Bundle());
        map.getMapAsync(gmap -> {
            googleMap = gmap;
            gmap.setMyLocationEnabled(true);
            map.onResume();
            map.post(() -> {
                gmap.addMarker(new MarkerOptions().position(source).title("Source")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                gmap.addMarker(new MarkerOptions().position(dest).title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                gmap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().include(source).include(dest).build(), 100));
                gmap.addPolyline(new PolylineOptions().color(Color.BLUE).add(source, dest));
            });
        });
    }

    public void rideHasStarted(){
        //allow our location updates to firebase
        ((HomeActivity)getActivity()).userInRide(false,ride.rideID);

        //set listeners for driver location
        driverLatRef = FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_DRIVER_LAT);
        driverLngRef = FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_DRIVER_LNG);

        driverLatRef.addValueEventListener(driverLatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try{
                    driverLat = (double) dataSnapshot.getValue();
                } catch(ClassCastException ex){}

                synchronized (lock){
                    if(driver==null){
                        driver = googleMap.addMarker(new MarkerOptions().title("Driver").icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_RED)).position(new LatLng(driverLat,driverLng)));
                    } else driver.setPosition(new LatLng(driverLat,driverLng));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        driverLngRef.addValueEventListener(driverLngListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try{
                    driverLng = (double) dataSnapshot.getValue();
                } catch (ClassCastException ex){}

                synchronized (lock){
                    if(driver==null){
                        driver = googleMap.addMarker(new MarkerOptions().title("Driver").icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_RED)).position(new LatLng(driverLat,driverLng)));
                    } else driver.setPosition(new LatLng(driverLat,driverLng));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //ride end listener is set in HomeActivity

    }

    public void rideHasNotStarted(){
        //set listener for when ride starts
        rideStartRef = FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_RIDE_STATUS);

        rideStartRef.addValueEventListener(rideStartListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getValue(String.class).equals(AppConstants.RIDE_STARTED)){
                            Util.shortToast(getActivity(),"Ride has started");
                            //reload this fragment
                            ((HomeActivity)getActivity()).goToFragment(R.id.myBookedRidesOption);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ((HomeActivity)context).getSupportActionBar().setTitle("My Booked Ride");
     }

    public void someErrorOccured() {
        Util.shortToast(getActivity(), "Something went wrong");
    }

    public void someErrorOccured(Exception ex) {
        Util.shortToast(getActivity(), "Error : " + ex.getMessage());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(driverLatRef!=null)driverLatRef.removeEventListener(driverLatListener);
        if(driverLngRef!=null)driverLngRef.removeEventListener(driverLngListener);

        if(rideStartRef!=null)rideStartRef.removeEventListener(rideStartListener);
    }
}
