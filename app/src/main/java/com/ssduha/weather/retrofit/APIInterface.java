package com.ssduha.weather.retrofit;

import com.ssduha.weather.WeatherModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface APIInterface {

    @GET("forecast.json")
    Call<WeatherModel> getData(@Query("key") String apiKey, @Query("q") String city, @Query("days") String day);
}
