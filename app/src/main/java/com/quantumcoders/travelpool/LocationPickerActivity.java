package com.quantumcoders.travelpool;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.quantumcoders.travelpool.utility.AppConstants;
import com.quantumcoders.travelpool.utility.Util;

public class LocationPickerActivity extends AppCompatActivity {

    GoogleMap googleMap = null;
    MapView mapView = null;
    Marker pickedLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::mapCallback);
        mapView.setClickable(true);

        findViewById(R.id.chooseButton).setOnClickListener((v)->{
            if(pickedLocation!=null){
                Intent data = new Intent();
                data.putExtra(AppConstants.LATITUDE, pickedLocation.getPosition().latitude);
                data.putExtra(AppConstants.LONGITUDE, pickedLocation.getPosition().longitude);
//                System.out.println("LOCATION RESULT " + pickedLocation.getPosition().latitude + " " + pickedLocation.getPosition().longitude);
                setResult(RESULT_OK,data);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        });

        Util.longToast(this,"Long Press on map to select a location");

    }


    public void mapCallback(GoogleMap map){
        googleMap = map;
        map.setMyLocationEnabled(true);
        map.setOnMapLongClickListener((e)->{
            if(pickedLocation != null) pickedLocation.remove();
            pickedLocation = map.addMarker(new MarkerOptions().position(new LatLng(e.latitude,e.longitude)).title("Pick"));
            pickedLocation.setVisible(true);
        });
        System.out.println("Map Callback");
        mapView.onResume();
    }

}
