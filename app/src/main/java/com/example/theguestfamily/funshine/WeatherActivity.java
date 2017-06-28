package com.example.theguestfamily.funshine;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.theguestfamily.funshine.model.DailyWeatherReport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

    final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast?";
    final String URL_COORD = "/?lat= &lon=";
    final String URL_UNITS = "&units=imperial"; //add a button to switch between imperial and metric
    final String URL_API_KEY = "&APPID=1c3014b8ebd808a6eb70a74ee86e4ae4";

    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSION_LOCATION = 111;
    private ArrayList<DailyWeatherReport> weatherReportList = new ArrayList<>();

    private ImageView weatherIcon;
    private ImageView weatherIconMini;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView cityCountry;
    private TextView weatherDescription;

    WeatherAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherIcon = (ImageView)findViewById(R.id.weatherIcon);
        weatherIconMini = (ImageView)findViewById(R.id.weatherIconMini);
        weatherDate = (TextView)findViewById(R.id.weatherDate);
        currentTemp = (TextView)findViewById(R.id.currentTemp);
        lowTemp = (TextView)findViewById(R.id.lowTemp);
        cityCountry = (TextView)findViewById(R.id.cityCountry);
        weatherDescription = (TextView)findViewById(R.id.weatherDescription);

        //Loads the weather list
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.content_weather_report);

        mAdapter = new WeatherAdapter(weatherReportList);

        recyclerView.setAdapter(mAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);

        recyclerView.setLayoutManager(layoutManager);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this,this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void downloadWeatherData(Location location) {
        final String fullCoords = URL_COORD + location.getLatitude() + "&lon=" + location.getLongitude();
        final String url = URL_BASE + fullCoords + URL_UNITS + URL_API_KEY;

        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //Log.v("FUN", "Response: " + response.toString());
                //Json parsing happens here
                try {

                    JSONObject city = response.getJSONObject("city");
                    String cityName = city.getString("name");
                    String country = city.getString("country");

                    JSONArray list = response.getJSONArray("list");

                    for (int x = 0; x < 5; x++) {
                        JSONObject obj = list.getJSONObject(x); //goes into json and opens first object, called list
                        JSONObject main = obj.getJSONObject("main"); //gets first object in list known as main
                        Double currentTemp = main.getDouble("temp");
                        Double maxTemp = main.getDouble("temp_max");
                        Double minTemp = main.getDouble("temp_min");

                        JSONArray weatherArr = obj.getJSONArray("weather"); //enters the weather array
                        JSONObject weather = weatherArr.getJSONObject(0); //gets first item
                        String weatherType = weather.getString("main"); //finds key matching "main"

                        String rawDate = obj.getString("dt_txt");

                        DailyWeatherReport report = new DailyWeatherReport(cityName,country,currentTemp.intValue(),maxTemp.intValue(),minTemp.intValue(),weatherType,rawDate);

                        weatherReportList.add(report);
                    }

                    Log.v("JSON", "NAME: " + cityName + " - " + "COUNTRY: " + country); //always test with log!
                } catch (JSONException e) {
                    Log.v("JSON", "Error: " + e.getLocalizedMessage());
                }

                updateUI();
                mAdapter.notifyDataSetChanged();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.v("FUN", "Error: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    public void updateUI() {
        if (weatherReportList.size() > 0) {
            DailyWeatherReport report = weatherReportList.get(0);

            switch (report.getWeather()) {
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    break;
                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
            }
            //weatherDate.setText("Today, June 28");
            currentTemp.setText(Integer.toString(report.getCurrentTemp()));
            lowTemp.setText(Integer.toString(report.getMinTemp()));
            cityCountry.setText(report.getCityName() + ", " + report.getCountry());
            weatherDescription.setText(report.getWeather());
        }
    }

    //These 4 methods below comply with the implementations of this class
    @Override
    public void onLocationChanged(Location location) {
        downloadWeatherData(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
        } else {
            startLocationServices();
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void startLocationServices() {
        try {
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,req,this);
        } catch (SecurityException exception) {

        }
    }

    //if permissions aren't set they are set by user here
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        switch (requestCode) {
            case PERMISSION_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationServices();
                } else {
                    //Error...
                    Toast.makeText(this,"Location can't be found - allow permission?",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public class WeatherAdapter extends RecyclerView.Adapter<weatherReportViewHolder> {

        private ArrayList<DailyWeatherReport> mDailyWeatherReports;

        public WeatherAdapter(ArrayList<DailyWeatherReport> dailyWeatherReports) {
            mDailyWeatherReports = dailyWeatherReports;
        }

        @Override
        public void onBindViewHolder(weatherReportViewHolder holder, int position) {
            DailyWeatherReport report = mDailyWeatherReports.get(position);
            holder.updateUI(report);
        }

        @Override
        public int getItemCount() {
            return weatherReportList.size();
        }

        @Override
        public weatherReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_weather, parent, false);
            return new weatherReportViewHolder(card);
        }
    }

    public class weatherReportViewHolder extends RecyclerView.ViewHolder {

        private ImageView lweatherIcon;
        private TextView lweatherDate;
        private TextView lweatherDescription;
        private TextView ltempHigh;
        private TextView ltempLow;

        public weatherReportViewHolder(View itemView) {
            super(itemView);

            lweatherIcon = (ImageView)itemView.findViewById(R.id.list_weather_icon);
            lweatherDate = (TextView)itemView.findViewById(R.id.list_weather_day);
            lweatherDescription = (TextView)itemView.findViewById(R.id.list_weather_description);
            ltempHigh = (TextView)itemView.findViewById(R.id.list_weather_temp_high);
            ltempLow = (TextView)itemView.findViewById(R.id.list_weather_temp_low);
        }

        public void updateUI(DailyWeatherReport report) {

            DateFormat format = new SimpleDateFormat("EEEE", Locale.getDefault());

            lweatherDate.setText(format.format(report.getFormattedDate()));
            lweatherDescription.setText(report.getWeather());
            ltempHigh.setText(Integer.toString(report.getMaxTemp()));
            ltempLow.setText(Integer.toString(report.getMinTemp()));

            switch (report.getWeather()) {

                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy_mini));
                    break;
                default:
                    lweatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
            }
        }
    }
}
