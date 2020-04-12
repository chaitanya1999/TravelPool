package com.quantumcoders.travelpool.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyOfferedRideFragment extends Fragment {

    View fragmentView;
    TextView tv_driverName, tv_date, tv_time, tv_vehicle, tv_extra, tv_phone, tv_rideStatus;
    TextView tv_bookingStatus, tv_bookerName, tv_bookerPhone;
    Button btnStartEndRide;
    MapView map;
    GoogleMap googleMap;
    LatLng source, dest;
    RideInfo ride;

    String email;
    Marker bookerMarker = null;

    //if bookerLocation doesn't changes it will help prevent setting the marker on map
    // for the first time when the function is called to get booker lat lng. i.e. it will prevent marker to appear on map if
    // ride isn't booked
    volatile double bookerLat, bookerLng;
    boolean bookerLatFlag = false;
    boolean bookerLngFlag = false;
    Object lock = new Object();


    DatabaseReference bookerNameRef,bookerPhoneRef;
    ValueEventListener bookerNameListener,bookerPhoneListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_my_offered_rides, container, false);

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

        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(email).child(AppConstants.DBKEY_USER_OFFERED_RIDE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String rideID = dataSnapshot.getValue(String.class);
                        System.out.println("Ride ID Obtained = " + rideID);
                        MyOfferedRideFragment.this.onObtainedRideID(rideID);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        someErrorOccured();
                    }
                });

        return fragmentView;
    }

    public void onObtainedRideID(String rideID) {
        if (rideID.equals("0")) {

            fragmentView.findViewById(R.id.offeredMessage).setVisibility(View.VISIBLE);

        } else {
            FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(rideID)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            RideInfo rideInfo = dataSnapshot.getValue(RideInfo.class);
                            initViewsWithRideInfo(rideInfo);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            someErrorOccured();
                        }
                    });
        }
    }

    public void initViewsWithRideInfo(RideInfo rideInfo) {
        //setup views and instance variables
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

        btnStartEndRide.setEnabled(true);
        if (rideInfo.getRideStatus().equals(AppConstants.RIDE_NOT_STARTED)) {

            btnStartEndRide.setOnClickListener(this::btnListenerStartRide);
            //^ the listener here will inherently call rideHasStarted function

        } else if (rideInfo.getRideStatus().equals(AppConstants.RIDE_STARTED)) {

            rideHasStarted();   //since ride has already started

        }


        // set listeners for ride booked after this fragment startsor after starting of ride
        bookerNameRef = FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_BOOKER_NAME);
        bookerPhoneRef = FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_BOOKER_PHONE);

        bookerNameRef.addValueEventListener( bookerNameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                System.out.println("DEBUG bookerNameRef : " + dataSnapshot.getValue());
                String value = dataSnapshot.getValue(String.class);
                if(value!=null && !value.trim().isEmpty()){
                    tv_bookingStatus.setText(AppConstants.BOOKED);
                    tv_bookerName.setText(value);
                    bookerNameRef.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        bookerPhoneRef.addValueEventListener( bookerPhoneListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String value = dataSnapshot.getValue(String.class);
                if(value!=null && !value.trim().isEmpty()){
                    tv_bookerPhone.setText(dataSnapshot.getValue(String.class));
                    bookerPhoneRef.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    //listener
    public void btnListenerStartRide(View view) {
        //set ride status to started
        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_RIDE_STATUS).setValue(AppConstants.RIDE_STARTED).addOnSuccessListener(aVoid -> {
            //set user state to riding ( driving )
            FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(email)
                    .child(AppConstants.DBKEY_USER_STATE).setValue(AppConstants.STATE_GIVING_RIDE).addOnSuccessListener(aVoid2 -> {

                //update state of booker
                if (ride.getBookingStatus().equals(AppConstants.BOOKED)) {
                    String bookerEmail = ride.getBookerEmail();
                    //set booker state
                    //call rideHasStarted() to set callbacks to update driver location on Firebase
                    FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(bookerEmail)
                            .child(AppConstants.DBKEY_USER_STATE).setValue(AppConstants.STATE_RIDING)
                            .addOnSuccessListener(this::_rideHasStarted)
                            .addOnFailureListener(this::someErrorOccured);
                } else {
                    rideHasStarted();
                }
                // if booker comes after ride starts, then status of booker will be updated by the MyBookedRideFragment on
                // booker's side
                // actually so there is no need to update booker state here
                // but it's better to not touch working piece of code
                // since it has no side effects then

            }).addOnFailureListener(this::someErrorOccured);
        }).addOnFailureListener(this::someErrorOccured);


    }

    //listener
    public void btnListenerEndRide(View view) {
        //firebase location value listener would be removed when this fragment ends
        //call to userNotInRide in HomeActivity would stop location updates to Firebase
        // so set driver , booker and ride states accordingly and other things that must be updated

        /* BEWARE MY FRIEND, THERE IS TOO MUCH CODE DOWN THERE IN THE RUNNABLE VALLEY. ITS A MOUNTAIN OF CODE.
        * JAVA IS REALLY NOT SUITED FOR ASYNCHRONOUS PROGRAMMING WHICH IS THE MAJOR CAUSE FOR THE HUMONGOUS PILE OF
        * CODE WRITTEN BELOW. PERHAPS I COULD HAVE DONE BETTER BY MAKING RUNNABLES FOR EACH NESTED
        * FIREBASE CALL THEN EXECUTING THEM ONE BY ONE ( OR MAYBE NOT xD LOL HAHAHAHA, SINCE THE
        * CALLS ARE DEPENDENT ON THE BASIS OF LAST CALL'S SUCCESS OR FAILURE, IT NEEDS TO BE NESTED. BUT YEAH IT CAN BE DONE TOO
        * AT THIS POINT I AM CRAZY. DONT JUDGE ME YOU LITTLE PRICK. )
        *  */
        Runnable tooMuchNestedCode = ()->{
            /*
            * LOL TOLD YA. ITS TOO MUCH NESTING. HAVE FUN EXPLORING THE DEPTHS OF THIS VALLEY. ITS BEAUTIFUL.
            *
            * */
            /*dealing with driver*/
            //first set driver state to idle
            FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(email)
                    .child(AppConstants.DBKEY_USER_STATE).setValue(AppConstants.STATE_IDLE)
                    .addOnSuccessListener(aVoid -> {
                        //now set the offeredRide of driver to "0"
                        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(email)
                                .child(AppConstants.DBKEY_USER_OFFERED_RIDE).setValue("0")
                                .addOnSuccessListener(aVoid1 -> {
                                    /*DEALING WITH BOOKER NOW*/
                                    //obtain the latest bookerEmail if in case ride was booked after starting
                                    FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    //booker email is obtained here
                                                    String bookerEmail = dataSnapshot.getValue(RideInfo.class).getBookerEmail();
                                                    String rideBooking = dataSnapshot.getValue(RideInfo.class).getBookingStatus();

                                                    if(rideBooking.equals(AppConstants.BOOKED)){//if there is a booker then
                                                        //set booker status to IDLE
                                                        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(bookerEmail)
                                                                .child(AppConstants.DBKEY_USER_STATE).setValue(AppConstants.STATE_IDLE)
                                                                .addOnSuccessListener(runnable -> {
                                                                    //set booker bookedride to "0"
                                                                    FirebaseDatabase.getInstance().getReference(AppConstants.DOC_USERDATA).child(bookerEmail)
                                                                            .child(AppConstants.DBKEY_USER_BOOKED_RIDE).setValue("0")
                                                                            .addOnSuccessListener(runnable1 -> {

                                                                                //set the user not in ride to stop location updates
                                                                                ((HomeActivity)MyOfferedRideFragment.this.getActivity()).userNotInRide();
                                                                                // ^ this automatically switches the fragment

                                                                            })
                                                                            .addOnFailureListener(ex -> {
                                                                                someErrorOccured(ex);
                                                                            });
                                                                }).addOnFailureListener(ex -> {
                                                            someErrorOccured(ex);
                                                        });
                                                    } else {    //if no booker found then directly do this
                                                        //set the user not in ride to stop location updates
                                                        ((HomeActivity)getActivity()).userNotInRide();
                                                        // ^ this automatically switches the fragment
                                                    }


                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                                            });

                                })
                                .addOnFailureListener(ex -> {
                                    MyOfferedRideFragment.this.someErrorOccured(ex);
                                });
                    })
                    .addOnFailureListener(ex -> {
                        MyOfferedRideFragment.this.someErrorOccured(ex);
                    });
        };

        //first we start with setting ride status to ENDED
        //then all other steps will be executed in the Runnable tooMuchNestedCode

        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_RIDE_STATUS)
                .setValue(AppConstants.RIDE_ENDED)
                .addOnSuccessListener((v)->{

                    tooMuchNestedCode.run();    //everything else is in here

                }).addOnFailureListener(ex->{someErrorOccured(ex);});


    }

    //just so that the same function can be used as a method reference for a callback in firebase listener
    public void _rideHasStarted(Void v) {
        rideHasStarted();
    }

    ValueEventListener latListener, lngListener;

    //used to set callbacks from HomeActivity for location updates of driver and passenger
    public void rideHasStarted() {
        btnStartEndRide.setOnClickListener(this::btnListenerEndRide);
        btnStartEndRide.setText("End Ride");
        ((HomeActivity) getActivity()).userInRide(true, ride.rideID);

        //get & update location of booker , irrespective of ride is booked or not
        latListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!bookerLatFlag) {
                    bookerLatFlag = true;
                } else {
                    bookerLat = (Double) dataSnapshot.getValue();
                    //if marker not added add it. else update its location.
                    synchronized (lock) {
                        if (bookerMarker == null) {
                            bookerMarker = googleMap.addMarker(new MarkerOptions().title("Booker")
                                    .position(new LatLng(bookerLat, bookerLng)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        } else {
                            bookerMarker.setPosition(new LatLng(bookerLat, bookerLng));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        lngListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!bookerLngFlag) {
                    bookerLngFlag = true;
                } else {
                    bookerLng = (Double) dataSnapshot.getValue();
                    //if marker not added add it. else update its location.
                    synchronized (lock) {
                        if (bookerMarker == null) {
                            bookerMarker = googleMap.addMarker(new MarkerOptions().title("Booker")
                                    .position(new LatLng(bookerLat, bookerLng)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        } else {
                            bookerMarker.setPosition(new LatLng(bookerLat, bookerLng));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_BOOKER_LAT).addValueEventListener(latListener);
        FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                .child(AppConstants.DBKEY_BOOKER_LNG).addValueEventListener(lngListener);

        //modify ui
        tv_rideStatus.setText(AppConstants.RIDE_STARTED);
//        //if ride gets booked in between
//        if (ride.getBookingStatus().equals(AppConstants.UNBOOKED)) {
//
//        }

    }

    public void someErrorOccured() {
        Util.shortToast(getActivity(), "Something went wrong");
    }

    public void someErrorOccured(Exception ex) {
        Util.shortToast(getActivity(), "Error : " + ex.getMessage());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ((HomeActivity)context).getSupportActionBar().setTitle("My Offered Ride");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(ride!=null && latListener!=null && lngListener!=null){
            FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                    .child(AppConstants.DBKEY_BOOKER_LAT).removeEventListener(latListener);
            FirebaseDatabase.getInstance().getReference(AppConstants.DOC_RIDESDATA).child(ride.getRideID())
                    .child(AppConstants.DBKEY_BOOKER_LNG).removeEventListener(lngListener);
        }

        if(bookerNameRef != null && bookerPhoneRef!=null){
            bookerNameRef.removeEventListener(bookerNameListener);
            bookerPhoneRef.removeEventListener(bookerPhoneListener);
        }
    }
}
