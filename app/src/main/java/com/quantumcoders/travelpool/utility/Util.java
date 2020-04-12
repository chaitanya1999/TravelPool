package com.quantumcoders.travelpool.utility;

import android.content.Context;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.quantumcoders.travelpool.utility.AppConstants;

public class Util {
    public static void longToast(Context ct, String msg){
        Toast.makeText(ct,msg,Toast.LENGTH_LONG).show();
    }
    public static void shortToast(Context ct, String msg){
        Toast.makeText(ct,msg,Toast.LENGTH_SHORT).show();
    }
    public static String encodeEmail(String email){
        return email.replace('.',',');
    }
    public static String decodeEmail(String email){
        return email.replace(',','.');
    }

    public static String sprefGetString(Context ct,String key){
        return ct.getApplicationContext().getSharedPreferences(AppConstants.SPREF_FILE,Context.MODE_PRIVATE).getString(key,null);
    }
    public static void sprefSetString(Context ct,String key,String value){
        ct.getApplicationContext().getSharedPreferences(AppConstants.SPREF_FILE,Context.MODE_PRIVATE).edit().putString(key,value).apply();
    }
    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
