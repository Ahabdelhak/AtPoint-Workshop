package atpoint_workshop.com;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import atpoint_workshop.com.Common.Common;
import atpoint_workshop.com.Helper.DirectionsJSONParser;
import atpoint_workshop.com.Model.FCMResponse;
import atpoint_workshop.com.Model.Notification;
import atpoint_workshop.com.Model.Sender;
import atpoint_workshop.com.Model.Token;
import atpoint_workshop.com.Remote.IFCMService;
import atpoint_workshop.com.Remote.IGoogleAPI;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Mech_Traking extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;

    double driverLat , driverlng ;

    private Circle driverMarker ;
    private Marker mechanicMarker ;

    private Polyline direction ;
    IGoogleAPI mService ;

    IFCMService mFCMService ;

    GeoFire geoFire;

    String customerId ;
    //play services
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mech__traking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(getIntent() != null){
            driverLat =getIntent().getDoubleExtra("lat" , -1.0 );
            driverlng =getIntent().getDoubleExtra("lng" , -1.0 );

            customerId = getIntent().getStringExtra("customerId");

        }
        mService = Common.getGoogleAPI();
        mFCMService = Common.getFcmService();

        setUpLocation();
    }

    private void setUpLocation() {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                creatLocationRequest();
                displayLocation();
                }

    }

    private void creatLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            } else {
                Toast.makeText(this, "This Device Is Not Supported", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        driverMarker = mMap.addCircle(new CircleOptions()
        .center(new LatLng(driverLat,driverlng))
        .radius(50) //50 m
        .strokeColor(Color.BLUE)
        .fillColor(0x220000FF)
        .strokeWidth(5.0f));


        //Notification When Mechanic arrive
        //Create Geo fecncing with radius 50 m
        geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(Common.mechanic_tbl));
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(driverLat,driverlng), 0.05f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

            //we need here customerid to send Notification
                SendArrivedNotification(customerId);

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });



    }

    //Arriving Location
    private void SendArrivedNotification(String customerId) {

        Token token = new Token(customerId);
        Notification notification = new Notification("Arrived" , String.format("The Mechanic %s has arrived at your location",Common.currentUser.getName()));

        Sender sender =new Sender(token.getToken(), notification);

        mFCMService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if(response.body().success != 1){
                    Toast.makeText(Mech_Traking.this , "Failed !" , Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });

    }

    private void startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    // TODO: 3/4/2018  mechanicMarker.remove() and direction.remove() return null !!
    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation != null) {

                final double latitud = Common.mLastLocation.getLatitude();
                final double logitude = Common.mLastLocation.getLongitude();

              // if(mechanicMarker != null) {
                    //mechanicMarker.remove();
                    mechanicMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitud, logitude))
                            .title("You")
                            .icon(BitmapDescriptorFactory.defaultMarker()));
                //}
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitud ,logitude),17.0f));

                    //if(direction != null){
                        //direction.remove(); //remove old direction
                        getDirection();
                    //}
            } else {

            Log.d("Error" , "Can't get your location");

        }
    }

    private void getDirection() {

        LatLng currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());
        String reuestApi = null;
        Toast.makeText(getApplication(), "done", Toast.LENGTH_LONG).show();
        try {
            reuestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination="+driverLat+","+driverlng +"&"+
                    "key=" + getResources().getString(R.string.google_directions_api);
            Log.d("AtPoint", reuestApi);
            //Toast.makeText(getApplication(), "done" + reuestApi, Toast.LENGTH_LONG).show();
            mService.getPath(reuestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {


                        new ParseTask().execute(response.body().toString());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(Mech_Traking.this, "error" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdate();

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation = location;
        displayLocation();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class ParseTask extends AsyncTask<String, Integer,List<List<HashMap<String,String>>>> {

        ProgressDialog mDialog = new ProgressDialog(Mech_Traking.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.setMessage("Please Waiting...");
            mDialog.show();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {

            JSONObject jobect ;
            List<List<HashMap<String, String>>> routes = null ;

            try {
                jobect = new JSONObject(strings[0]);

                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jobect);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();

            ArrayList points = null ;
            PolylineOptions polylineOptions= null ;

            for (int i = 0 ; i< lists.size() ; i++ ) {

                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = lists.get(i);

                for(int j = 0 ; j< path.size(); j++){

                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat,lng);

                    points.add(position);
                }
                polylineOptions.addAll(points);
                polylineOptions.width(10);
                polylineOptions.color(Color.RED);
                polylineOptions.geodesic(true);
            }
            direction = mMap.addPolyline(polylineOptions);
        }
    }
}