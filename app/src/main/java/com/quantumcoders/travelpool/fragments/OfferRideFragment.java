package com.quantumcoders.travelpool.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.quantumcoders.travelpool.utility.AppConstants;
import com.quantumcoders.travelpool.HomeActivity;
import com.quantumcoders.travelpool.LocationPickerActivity;
import com.quantumcoders.travelpool.R;
import com.quantumcoders.travelpool.utility.RideInfo;
import com.quantumcoders.travelpool.utility.UserInfo;
import com.quantumcoders.travelpool.utility.Util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class OfferRideFragment extends Fragment {
    Button btnPickSource,btnPickDest,btnOfferRide;
    EditText dateChooser,timeChooser;
    EditText tvName,vehicleDetails,extraDetails;
    TextView offeredMessage;

    final int REQCODE_PICK_SOURCE = 103;
    final int REQCODE_PICK_DEST = 104;

    LatLng source,dest;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_offer_ride, container, false);
        btnPickSource = view.findViewById(R.id.pickSourceBtn);
        btnPickDest = view.findViewById(R.id.pickDestBtn);
        btnOfferRide = view.findViewById(R.id.btnOfferRide);
        dateChooser = view.findViewById(R.id.dateChooser);
        timeChooser = view.findViewById(R.id.timeChooser);

        tvName = view.findViewById(R.id.tvName);
        extraDetails = view.findViewById(R.id.extraDetails);
        vehicleDetails = view.findViewById(R.id.vehicleDetails);

        offeredMessage = view.findViewById(R.id.offeredMessage);

        btnPickSource.setOnClickListener(this::pickLocation);
        btnPickDest.setOnClickListener(this::pickLocation);
        btnOfferRide.setOnClickListener(this::offerRide);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        dateChooser.setOnClickListener(this::dateTimeListener);
        timeChooser.setOnClickListener(this::dateTimeListener);

        FirebaseDatabase.getInstance().getReference("userData/" + Util.encodeEmail(Util.sprefGetString(getActivity(), AppConstants.EMAIL_KEY)))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        System.out.println("Data = " + dataSnapshot.getValue(UserInfo.class).getOfferedRide());
                        if(!dataSnapshot.getValue(UserInfo.class).getOfferedRide().equals("0")){
                            offeredMessage.setVisibility(View.VISIBLE);
                            btnPickDest.setEnabled(false);
                            btnPickSource.setEnabled(false);
                            btnOfferRide.setEnabled(false);

                            dateChooser.setEnabled(false);
                            timeChooser.setEnabled(false);
                            tvName.setEnabled(false);
                            extraDetails.setEnabled(false);
                            vehicleDetails.setEnabled(false);
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
        ((AppCompatActivity)context).getSupportActionBar().setTitle("Offer A ride");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==REQCODE_PICK_SOURCE){
            if(resultCode== Activity.RESULT_OK) {
                source = new LatLng(data.getDoubleExtra(AppConstants.LATITUDE,0),data.getDoubleExtra(AppConstants.LONGITUDE,0));
                btnPickSource.setText("Location Picked");
                btnPickSource.setEnabled(false);
            }
        } else if(requestCode == REQCODE_PICK_DEST){
            if(resultCode== Activity.RESULT_OK) {
                dest = new LatLng(data.getDoubleExtra(AppConstants.LATITUDE,0),data.getDoubleExtra(AppConstants.LONGITUDE,0));
                btnPickDest.setText("Location Picked");
                btnPickDest.setEnabled(false);
            }
        }
    }

    public void dateTimeListener(View v){
        Calendar cal = Calendar.getInstance();
        if(v==dateChooser){
            DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    dateChooser.setText(day+"/"+month+"/"+year);
                }
            },cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH));
        } else if(v==timeChooser){
            TimePickerDialog dialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int hh, int mm) {
                    timeChooser.setText(hh+":"+mm);
                }
            },cal.get(Calendar.HOUR),cal.get(Calendar.MINUTE),true);
        }
    }

    public void pickLocation(View view){
        Intent intent = new Intent(getActivity(), LocationPickerActivity.class);
        if(view==btnPickSource)startActivityForResult(intent,REQCODE_PICK_SOURCE);
        else if(view==btnPickDest)startActivityForResult(intent,REQCODE_PICK_DEST);
    }

    public void offerRide(View view){
        String yourName = tvName.getText().toString();
        String date = dateChooser.getText().toString();
        String time = timeChooser.getText().toString();
        String vehicleDetails = this.vehicleDetails.getText().toString();
        String extraDetails = this.extraDetails.getText().toString();
        String rideID = "ride-" + new SimpleDateFormat("yyMMddhhmmssMs").format(new Date());

        String driverPhone = Util.sprefGetString(getActivity(),AppConstants.PHONE_KEY);
        String driverEmail = Util.sprefGetString(getActivity(),AppConstants.EMAIL_KEY);

        if(driverEmail==null || driverEmail.isEmpty()) System.out.println("Problem spref - "+driverEmail);

        driverEmail = Util.encodeEmail(driverEmail);

        if(yourName.isEmpty() || date.isEmpty() || time.isEmpty() || vehicleDetails.isEmpty() || extraDetails.isEmpty()
         || source == null || dest == null){
            Util.shortToast(getActivity(),"Please fill all the entries");
        } else {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ridesData").child(rideID);
            RideInfo ride = new RideInfo();
            ride.setDriverName(yourName);
            ride.setDate(date);
            ride.setTime(time);
            ride.setVehicleDetails(vehicleDetails);
            ride.setExtraDetails(extraDetails);
            ride.setRideID(rideID);
            ride.setRideStatus(AppConstants.RIDE_NOT_STARTED);

            ride.setDestLat(dest.latitude);
            ride.setDestLng(dest.longitude);
            ride.setSourceLat(source.latitude);
            ride.setSourceLng(source.longitude);

            ride.setBookingStatus(AppConstants.UNBOOKED);
            ride.setDriverEmail(driverEmail);
            ride.setDriverPhone(driverPhone);

            String finalDriverEmail = driverEmail;
            ref.setValue(ride).addOnSuccessListener(aVoid -> {
                FirebaseDatabase.getInstance().getReference("userData/").child(finalDriverEmail).child(AppConstants.DBKEY_USER_OFFERED_RIDE)
                        .setValue(rideID);
                Util.shortToast(getActivity(),"Ride Offered Successfully");
                ((HomeActivity)getActivity()).goToFragment(R.id.offerRideOption);
            }).addOnFailureListener(ex ->{
                Util.shortToast(getActivity(),"An error occured " + ex.getMessage());
                System.out.println(ex);
            });
        }
    }


}
