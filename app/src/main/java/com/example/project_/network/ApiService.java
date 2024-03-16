package com.example.project_.network;

import com.example.project_.models.TimeModel;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface ApiService {

    @POST("send")
    Call<String> sendMessage(
            @HeaderMap HashMap<String, String> headers,
            @Body String messageBody
    );

    @GET("current/ip?ipAddress=237.71.232.203")
    Call<TimeModel> getTime();

}
