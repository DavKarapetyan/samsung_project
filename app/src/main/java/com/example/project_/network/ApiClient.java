package com.example.project_.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {

    private static Retrofit retrofitForNotification = null;
    private static Retrofit retrofitForTime = null;
    private static String TIME_URL = "https://timeapi.io/api/Time/";

    public static Retrofit getClient() {
        if(retrofitForNotification == null) {
            retrofitForNotification = new Retrofit.Builder()
                    .baseUrl("https://fcm.googleapis.com/fcm/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofitForNotification;
    }
    public static Retrofit getRetrofitInstance() {
        if (retrofitForTime == null) {
            retrofitForTime = new Retrofit.Builder()
                    .baseUrl(TIME_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitForTime;
    }
}
