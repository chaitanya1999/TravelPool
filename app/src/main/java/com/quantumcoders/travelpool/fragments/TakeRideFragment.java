package com.quantumcoders.travelpool.fragments;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.quantumcoders.travelpool.utility.AppConstants;
import com.quantumcoders.travelpool.HomeActivity;
import com.quantumcoders.travelpool.LocationPickerActivity;
import com.quantumcoders.travelpool.R;
import com.quantumcoders.travelpool.utility.RideInfo;
import com.quantumcoders.travelpool.RideListAdapter;
import com.quantumcoders.travelpool.utility.UserInfo;
import com.quantumcoders.travelpool.utility.Util;

import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;
import static com.quantumcoders.travelpool.utility.AppConstants.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class TakeRideFragment extends Fragment {
    Button pickSource,pickDest;
    EditText rangeText;
    Button searchRides;
    RecyclerView rideRecycler;
    TextView bookedMessage;

    final int REQCODE_SOURCE = 105;
    final int REQCODE_DEST = 106;

    LatLng source,dest;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_take_ride, container, false);

        pickSource = view.findViewById(R.id.pickSourceBtn);
        pickDest = view.findViewById(R.id.pickDestBtn);
        rangeText = view.findViewById(R.id.range);
        searchRides = view.findViewById(R.id.btnSearchRides);
        rideRecycler = view.findViewById(R.id.rideRecycler);
        bookedMessage = view.findViewById(R.id.bookedMessage);

        //check if ride is already booked or not and enable disable views accordingly
        String userEmail = Util.sprefGetString(getActivity(), AppConstants.EMAIL_KEY);
        userEmail = Util.encodeEmail(userEmail);

        FirebaseDatabase.getInstance().getReference(DOC_USERDATA).child(userEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //if ride already booked
                        if(!dataSnapshot.getValue(UserInfo.class).getBookedRide().equals("0")){
                            pickSource.setEnabled(false);
                            pickDest.setEnabled(false);
                            rangeText.setEnabled(false);
                            searchRides.setEnabled(false);
                            bookedMessage.setVisibility(View.VISIBLE);
                        } else {
                            //do nothing
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        searchRides.setOnClickListener(this::searchForRides);

        pickSource.setOnClickListener(this::pickLocation);
        pickDest.setOnClickListener(this::pickLocation);

        rideRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ((AppCompatActivity)context).getSupportActionBar().setTitle("Take A Ride");
    }

    public void pickLocation(View view){
        if(view==pickDest)startActivityForResult(new Intent(getActivity(), LocationPickerActivity.class), REQCODE_DEST);
        else if(view==pickSource)startActivityForResult(new Intent(getActivity(), LocationPickerActivity.class), REQCODE_SOURCE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==REQCODE_SOURCE){
            if(resultCode== RESULT_OK){
                source = new LatLng(data.getDoubleExtra(LATITUDE,0),data.getDoubleExtra(LONGITUDE,0));
                pickSource.setText("Location Picked");
//                pickSource.setEnabled(false);
            }
        } else if(requestCode==REQCODE_DEST){
            if(resultCode== RESULT_OK){
                dest = new LatLng(data.getDoubleExtra(LATITUDE,0),data.getDoubleExtra(LONGITUDE,0));
                pickDest.setText("Location Picked");
//                pickDest.setEnabled(false);
            }
        } else if(requestCode == REQCODE_BOOK_RIDE) {
            if(resultCode==RESULT_OK){
                ((HomeActivity)getActivity()).goToFragment(R.id.takeRideOption);
                Util.shortToast(getActivity(),"Ride Booked Successfully");
            }
        }
    }

    public void searchForRides(View view){
        if(source==null || dest==null || rangeText.getText().toString().isEmpty()) {
            Util.shortToast(getActivity(),"Please fill all the entries");
            return;
        } else {
            final int range = Integer.parseInt(rangeText.getText().toString());
            FirebaseDatabase.getInstance().getReference(DOC_RIDESDATA).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ArrayList<RideInfo> rideList = new ArrayList<>();
                    int count = 0;
                    String userEmail = Util.sprefGetString(getActivity(), EMAIL_KEY);
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        RideInfo ride = (child.getValue(RideInfo.class));
                        String rideDriverEmail = Util.decodeEmail(ride.getDriverEmail());
                        if(!ride.getRideStatus().equals(RIDE_ENDED) && !ride.getBookingStatus().equals(BOOKED)
                                && !userEmail.equals(rideDriverEmail)){
                            float distSources[] = new float[1];
                            float distDest[] = new float[1];
                            Location.distanceBetween(source.latitude,source.longitude,ride.getSourceLat(),ride.getSourceLng(),distSources);
                            Location.distanceBetween(dest.latitude,dest.longitude,ride.getDestLat(),ride.getDestLng(),distSources);

                            if(distDest[0]<=range && distSources[0]<=range){
                                rideList.add(ride);
                            }
                        }
                        count++;
                    }
                    System.out.println("RIDES : found " + count + " rides");
                    System.out.println("FILTERED RIDES : " + rideList.size());

                    //update recycler view
                    RideListAdapter adapter = new RideListAdapter(rideList);
                    rideRecycler.setAdapter(adapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }
}
