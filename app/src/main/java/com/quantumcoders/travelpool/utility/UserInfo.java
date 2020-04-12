package com.quantumcoders.travelpool.utility;

public class UserInfo {
    public String email,phoneNo,offeredRide,bookedRide,state;

    public UserInfo(){}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getOfferedRide() {
        return offeredRide;
    }

    public void setOfferedRide(String offeredRide) {
        this.offeredRide = offeredRide;
    }

    public String getBookedRide() {
        return bookedRide;
    }

    public void setBookedRide(String bookedRide) {
        this.bookedRide = bookedRide;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
