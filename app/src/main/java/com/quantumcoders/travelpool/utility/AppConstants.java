package com.quantumcoders.travelpool.utility;

public class AppConstants {
    //private constructor
    private AppConstants(){}


    /*
    * TIPS FOR NEXT TIME YOU MAKE SOME PROJECT
    * MAKE DIFFERENT CLASSES OR ENUMs WHEN DEALING WITH STATES I.E. LIKE IN THIS CASE RIDE STATES AND USER STATES
    * AND DIFFERENT CLASS/INTERFACE FOR DATABASE KEYS, SHARED PREFERENCES KEYS.
    * THE CODE BECOMES MORE READABLE IN THAT CASE.
    * INSTEAD OF THE STATEMENT AppConstants.STATE_IDLE or AppConstants.RIDE_STARTED the following calls are much more readable and
    * intuitive.
    *
    * User.IDLE
    * Ride.STARTED
    * DB.USERDATA
    * */


    public static final String SPREF_FILE = "spref";
    public static final String EMAIL_KEY = "email";
    public static final String PWD_KEY = "password";
    public static final String PHONE_KEY = "phone";
    public static final String LATITUDE = "lat";
    public static final String LONGITUDE = "lng";

    public static final String DOC_USERDATA = "userData";
    public static final String DOC_RIDESDATA = "ridesData";
    public static final String DBKEY_PHONE="phoneNo";
    public static final String DBKEY_USER_OFFERED_RIDE = "offeredRide";
    public static final String DBKEY_USER_BOOKED_RIDE = "bookedRide";
    public static final String DBKEY_RIDE_STATUS = "rideStatus";
    public static final String DBKEY_USER_STATE = "state";
    public static final String DBKEY_DRIVER_LAT = "driverLat";
    public static final String DBKEY_DRIVER_LNG = "driverLng";
    public static final String DBKEY_BOOKER_LAT = "bookerLat";
    public static final String DBKEY_BOOKER_LNG = "bookerLng";
    public static final String DBKEY_BOOKER_NAME = "bookerName";
    public static final String DBKEY_BOOKER_PHONE = "bookerPhone";

    /*   KEEP IN MIND NEVER COPY PASTE CODE. I COPY PASTED THE ABOVE VARIABLE DECLARATIONS, TO AVOID TYPING SAME THING AGAIN AND AGAIN
    "PUBLIC STATIC FINAL STRING DBKEY_"
     THOUGH I CHANGED VARIABLE NAMES BUT I FORGOT TO CHANGE THE STRING CONSTANTS VALUE WHICH RESULTED IN SEVERE BUGS  */

    public static final String STATE_IDLE = "idle";
    public static final String STATE_RIDING = "riding";
    public static final String STATE_GIVING_RIDE = "givingRide";


    public static final String UNBOOKED = "unbooked";
    public static final String BOOKED = "booked";

    public static final String RIDE_NOT_STARTED = "Ride not started yet";
    public static final String RIDE_STARTED = "Ride started";
    public static final String RIDE_ENDED = "Ride has ended";

    public static final int REQCODE_BOOK_RIDE = 107;

    public static final int PICK_LOC_REQCODE = 102;

}
