package com.ssduha.weather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.squareup.picasso.Picasso;
import com.ssduha.weather.retrofit.APIClient;
import com.ssduha.weather.retrofit.APIInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private TextView cityTV, tempTV, conditionTV;
    private ImageView searchIV, iconIV, gpsIV;
    private RecyclerView forecastRV;
    private ProgressBar progressBar;
    private EditText cityEdit;
    private ScrollView scrollViewHome;

    private FusedLocationProviderClient client;
    private final int PERMISSION_CODE = 100;
    private final int GPS_PERMISSION_CODE = 200;

    private ForecastAdapter forecastAdapter;
    private final List<Hour> forecastDataList = new ArrayList<>();
    private static final String apiKey = "814e12d85203413aab5134930213008";
    private static final String days = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityTV = findViewById(R.id.cityTV);
        tempTV = findViewById(R.id.tempTV);
        conditionTV = findViewById(R.id.conditionTV);
        searchIV = findViewById(R.id.searchIV);
        iconIV = findViewById(R.id.iconIV);
        gpsIV = findViewById(R.id.gpsIV);
        forecastRV = findViewById(R.id.forecastRV);
        progressBar = findViewById(R.id.progressBar);
        cityEdit = findViewById(R.id.cityEdit);
        scrollViewHome = findViewById(R.id.scrollView);


        client = LocationServices.getFusedLocationProviderClient(this);

        /*Save City Name*/
        SharedPreferences prefs = getSharedPreferences("CityPrefs", MODE_PRIVATE);
        String cityName = prefs.getString("cityName", "No name");

        /*App Permission*/
        if(cityName.equals("No name")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
                } else {
                    locationRequest();
                }
            } else {
                //if os is < marshmallow
                locationRequest();
            }
        }

        /*Save City Name Weather*/
        if(!cityName.equals("No name")) {
            getWeatherData(cityName);
        }


        searchIV.setOnClickListener(view -> getSearchCity());
        cityEdit.setOnKeyListener((view, i, keyEvent) -> {
            if(keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER){
                getSearchCity();
            }
            return false;
        });
        gpsIV.setOnClickListener(view -> locationRequest());

        forecastRV.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        forecastAdapter = new ForecastAdapter(forecastDataList, MainActivity.this);
        forecastRV.setAdapter(forecastAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_CODE && (grantResults.length > 0) && (grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED)){
            locationRequest();
        } else {
            /*Permission Not Granted*/
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
            new Handler().postDelayed(this::finish, 2000);
        }
    }

    @SuppressLint("MissingPermission")
    private void locationRequest() {
        LocationManager locationManager = (LocationManager) getSystemService(MainActivity.LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            try{
                LocationRequest locationRequest = new LocationRequest()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(10000)
                        .setFastestInterval(5000);

                LocationCallback locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();

                        String city = getCityName(location.getLongitude(), location.getLatitude());
                        getWeatherData(city);

                        /*Save City Name*/
                        SharedPreferences.Editor editor = getSharedPreferences("CityPrefs", MODE_PRIVATE).edit();
                        editor.putString("cityName", city);
                        editor.apply();
                    }
                };

                client.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            } catch (Exception e){
                e.printStackTrace();
            }
        } else{
            /*GPS Enable Message*/
            new AlertDialog.Builder(this)
                    .setTitle("GPS Permission")
                    .setMessage("GPS is required for this app. Please enable GPS")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, GPS_PERMISSION_CODE);
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GPS_PERMISSION_CODE) {
            LocationManager locationManager = (LocationManager) getSystemService(MainActivity.LOCATION_SERVICE);

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                /*GPS Enable*/
                locationRequest();
            } else {
                /*GPS Not Enable*/
                getWeatherData("No name");
                Toast.makeText(this, "GPS is not enable", Toast.LENGTH_LONG).show();
            }
        }
    }


    private String getCityName(double longitude, double latitude){
        String cityName = "City Not Found..";

        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
        try{
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 10);
            for(Address adr : addresses){
                String city = adr.getLocality();
                if(city != null && !city.equals("")){
                    cityName = city;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cityName;
    }

    private void getSearchCity(){
        String city = cityEdit.getText().toString();
        if (city.isEmpty()) {
            Toast.makeText(MainActivity.this, getString(R.string.cityName), Toast.LENGTH_LONG).show();
        } else {
            getWeatherData(city);
        }
    }

    private void getWeatherData(String city){
        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);
        Call<WeatherModel> call = apiInterface.getData(apiKey, city, days);

        /*Check Net Connection*/
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNet = manager.getActiveNetworkInfo();
        if(null != activeNet){

            call.enqueue(new Callback<WeatherModel>() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onResponse(Call<WeatherModel> call, Response<WeatherModel> response) {
                    progressBar.setVisibility(View.GONE);
                    scrollViewHome.setVisibility(View.VISIBLE);
                    forecastDataList.clear();
                    forecastAdapter.notifyDataSetChanged();

                    /*Current Weather*/
                    WeatherModel weatherModel = response.body();
                    try {
                        cityTV.setText(weatherModel.getLocation().getName());
                        tempTV.setText(weatherModel.getCurrent().getTemp_c() +"°C");
                        conditionTV.setText(weatherModel.getCurrent().getCondition().getText());
                        Picasso.get().load("https:".concat(weatherModel.getCurrent().getCondition().getIcon())).into(iconIV);
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    /*Forecast Weather Array List*/
                    List<Hour> hourArray = weatherModel.getForecast().getForecastday().get(0).getHour();
                    for (int i=0; i<hourArray.toArray().length; i++){
                        String time = hourArray.get(i).getTime();
                        Double temp = hourArray.get(i).getTemp_c();
                        String icon = hourArray.get(i).getCondition().getIcon();
                        Double wind = hourArray.get(i).getWind_kph();

                        forecastDataList.add(new Hour(time, temp, new Condition__2(icon), wind));
                    }

                }

                @Override
                public void onFailure(Call<WeatherModel> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
        }
    }
}