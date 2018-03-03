package atpoint_workshop.com;

import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import atpoint_workshop.com.Common.Common;
import atpoint_workshop.com.Model.Token;
import atpoint_workshop.com.Remote.IGoogleAPI;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Emergency extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;

    //play services
    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;
    private MaterialAnimatedSwitch location_switch;

    DatabaseReference mechanics;
    GeoFire geoFire;
    Marker mCurrent;

    SupportMapFragment mapFragment;

    //Car Animation
    private List<LatLng> PolyLineList;
    private Marker carMarker;
    private float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPosition, endPosition, currentPosition;
    private int index, next;
    private PlaceAutocompleteFragment places;
    private String destination;
    private PolylineOptions polylineOptions, blackpolylineOptions;
    private Polyline blackPolyline, greyPolyline;
    private IGoogleAPI mService;

    DatabaseReference onlineRef,currentUserRef;

    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
            if (index < PolyLineList.size() - 1) {
                index++;
                next = index + 1;
            }
            if (index < PolyLineList.size() - 1) {
                startPosition = PolyLineList.get(index);
                endPosition = PolyLineList.get(next);
            }
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    v = valueAnimator.getAnimatedFraction();
                    lng = v * endPosition.longitude + (1 - v) * startPosition.longitude;
                    lat = v * endPosition.latitude + (1 - v) * startPosition.latitude;
                    LatLng newPos = new LatLng(lat, lng);
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f, 0.5f);
                    carMarker.setRotation(getBearing(startPosition, newPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(newPos).zoom(15.5f).build()
                    ));

                }
            });
            valueAnimator.start();
            handler.postDelayed(this, 3000);

        }
    };

    private float getBearing(LatLng startPosition, LatLng newPos) {
        double lat = Math.abs(startPosition.latitude - endPosition.latitude);
        double lng = Math.abs(startPosition.longitude - endPosition.longitude);
        if (startPosition.latitude < endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float) (90 - Math.toDegrees(Math.atan(lng / lat)) + 90);
        else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (startPosition.latitude < endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float) ((90 + Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Presence System
        onlineRef= FirebaseDatabase.getInstance().getReference().child(".info/connected");
        currentUserRef= FirebaseDatabase.getInstance().getReference(Common.mechanic_tbl)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //we will remove  value from  mechanic  tbl when mechanic disconnected
                currentUserRef.onDisconnect().removeValue();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //init View
        location_switch = (MaterialAnimatedSwitch) findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {
                if (isOnline) {
                    FirebaseDatabase.getInstance().goOnline();//set connected  when switch to on
                    startLocationUpdate();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "Your Are Online", Snackbar.LENGTH_SHORT).show();
                } else {
                    FirebaseDatabase.getInstance().goOffline();//set disconnected when swith is off
                    stopLocationUpdates();
                    //handler.removeCallbacks(drawPathRunnable);
                    Snackbar.make(mapFragment.getView(), "Your Are Offline", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        //Car Animation
        PolyLineList = new ArrayList<>();
        places = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        places.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (location_switch.isChecked()) {
                    destination = place.getAddress().toString();
                    destination = destination.replace(" ", "+");
                    Log.d("Umair", destination);
                    getDirection();
                } else {
                    Toast.makeText(getApplication(), "Please Change Your Status To Online", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Toast.makeText(getApplication(), "" + status.toString(), Toast.LENGTH_SHORT).show();
            }
        });


        mechanics = FirebaseDatabase.getInstance().getReference().child(Common.mechanic_tbl);
        geoFire = new GeoFire(mechanics);
        setUpLocation();

        mService = Common.getGoogleAPI();

        updateFirebaseToken();
    }

    private void updateFirebaseToken() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_table);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        if (FirebaseAuth.getInstance().getCurrentUser() != null)//if already login, must update tokens
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
        {

        }
    }

    private void getDirection() {
        currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());
        String reuestApi = null;
        Toast.makeText(getApplication(), "done", Toast.LENGTH_LONG).show();
        try {
            reuestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination=" + destination + "&" +
                    "key=" + getResources().getString(R.string.google_directions_api);
            Log.d("AtPoint", reuestApi);
            //Toast.makeText(getApplication(), "done" + reuestApi, Toast.LENGTH_LONG).show();
            mService.getPath(reuestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject routes = jsonArray.getJSONObject(i);
                            JSONObject poly = routes.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            PolyLineList = decodePoly(polyline);
                        }
                        //Adjust Bounds
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (LatLng latLng : PolyLineList) {
                            builder.include(latLng);
                            LatLngBounds bounds = builder.build();
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                            mMap.animateCamera(cameraUpdate);

                            polylineOptions = new PolylineOptions();
                            polylineOptions.color(Color.GRAY);
                            polylineOptions.width(5);
                            polylineOptions.startCap(new SquareCap());
                            polylineOptions.endCap(new SquareCap());
                            polylineOptions.jointType(JointType.ROUND);
                            polylineOptions.addAll(PolyLineList);
                            greyPolyline = mMap.addPolyline(polylineOptions);

                            blackpolylineOptions = new PolylineOptions();
                            blackpolylineOptions.color(Color.BLACK);
                            blackpolylineOptions.width(5);
                            blackpolylineOptions.startCap(new SquareCap());
                            blackpolylineOptions.endCap(new SquareCap());
                            blackpolylineOptions.jointType(JointType.ROUND);
                            blackpolylineOptions.addAll(PolyLineList);
                            blackPolyline = mMap.addPolyline(blackpolylineOptions);

                            mMap.addMarker(new MarkerOptions()
                                    .position(PolyLineList.get(PolyLineList.size() - 1))
                                    .title("PickUp Location"));
                            //Animation
                            ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0, 100);
                            polyLineAnimator.setDuration(2000);
                            polyLineAnimator.setInterpolator(new LinearInterpolator());
                            polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                    List<LatLng> points = greyPolyline.getPoints();
                                    int percentValue = (int) valueAnimator.getAnimatedValue();
                                    int size = points.size();
                                    int newPoints = (int) (size * (percentValue / 100.0f));
                                    List<LatLng> p = points.subList(0, newPoints);
                                    blackPolyline.setPoints(p);

                                }
                            });
                            polyLineAnimator.start();

                            carMarker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                            handler = new Handler();
                            index = -1;
                            next = 1;
                            handler.postDelayed(drawPathRunnable, 3000);


                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(Emergency.this, "error" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Decode PolyLine
    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
    //Because We Need Runtime permisssions So overwrite the onRequestPermissionResult mthod


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                        creatLocationRequest();
                        if (location_switch.isChecked()) {
                            displayLocation();
                        }
                    }
                }
        }
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Request Runtime Permission
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                creatLocationRequest();
                if (location_switch.isChecked()) {
                    displayLocation();
                }
            }
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


    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation != null) {
            if (location_switch.isChecked()) {
                final double latitud = Common.mLastLocation.getLatitude();
                final double logitude = Common.mLastLocation.getLongitude();

                //Update to firebase
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitud, logitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (mCurrent != null) {
                            mCurrent.remove();
                            //Toast.makeText(Welcome.this, "Lun Hai Mera2", Toast.LENGTH_SHORT).show();
                            mCurrent = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitud, logitude))
                                    .title("You"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitud, logitude), 15.0f));
                            //Draw Animation rotate amrker
                            // rotateMarker(mCurrent, -360, mMap);
                        } else {
                            //Toast.makeText(Welcome.this, "Lun Hai Mera", Toast.LENGTH_SHORT).show();
                            mCurrent = mMap.addMarker(new MarkerOptions().position(new LatLng(latitud, logitude))
                                    .title("You"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitud, logitude), 15.0f));
                            //Draw Animation rotate amrker
                            //rotateMarker(mCurrent, -360, mMap);
                        }

                    }
                });
            } else {
                Snackbar.make(mapFragment.getView(), "Please Turn On Your Search", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Snackbar.make(mapFragment.getView(), "Cannot Get Your Location", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = mCurrent.getRotation();
        final long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                float rot = t * i + (1 - t) * startRotation;
                mCurrent.setRotation(-rot > 180 ? rot / 2 : rot);
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
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


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

    }

    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation = location;
        displayLocation();
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
