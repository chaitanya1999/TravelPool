package com.quantumcoders.travelpool;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.quantumcoders.travelpool.utility.AppConstants;
import com.quantumcoders.travelpool.utility.RideInfo;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RideListAdapter extends RecyclerView.Adapter<RideListAdapter.MyViewHolder> {

    ArrayList<RideInfo> dataset = new ArrayList<>();
    public RideListAdapter(ArrayList<RideInfo> list){
        dataset = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_ride,parent,false);
        MyViewHolder viewHolder = new MyViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        View v = holder.view;
        ((TextView)v.findViewById(R.id.tv_driverName)).setText(dataset.get(position).getDriverName());
        ((TextView)v.findViewById(R.id.tv_date)).setText(dataset.get(position).getDate());
        ((TextView)v.findViewById(R.id.tv_time)).setText(dataset.get(position).getTime());
        ((TextView)v.findViewById(R.id.tv_driverPhone)).setText(dataset.get(position).getDriverPhone());
        ((TextView)v.findViewById(R.id.tv_rideStatus)).setText(dataset.get(position).getRideStatus());
        holder.ride=dataset.get(position);
        v.setOnClickListener((view)->{
            Intent intent = new Intent(v.getContext(), RideBookingActivity.class);
            intent.putExtra("RideInfo",holder.ride);
            ((Activity)v.getContext()).startActivityForResult(intent, AppConstants.REQCODE_BOOK_RIDE);
        });
        //set on click listener
        //..
        //..
        //..
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        View view;
        RideInfo ride;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }
    }
}
