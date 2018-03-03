package atpoint_workshop.com;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import atpoint_workshop.com.Common.Common;
import atpoint_workshop.com.Model.FCMResponse;
import atpoint_workshop.com.Model.Notification;
import atpoint_workshop.com.Model.Sender;
import atpoint_workshop.com.Model.Token;
import atpoint_workshop.com.Remote.IFCMService;
import atpoint_workshop.com.Remote.IGoogleAPI;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ah_abdelhak on 2/28/2018.
 */
public class CustommerCall extends AppCompatActivity {

    private TextView tv_time, tv_distance, tv_address;
    private CircleImageView circleImageView;
    private MediaPlayer mediaPlayer;
    private Button btn_decline,btn_accept;
    IGoogleAPI mService;
    IFCMService mFCMService;

    double lat ,lng ;

    String customerId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custommer_call);

        mService = Common.getGoogleAPI();
        mFCMService=Common.getFcmService();
        //init views
        tv_time = (TextView) findViewById(R.id.txtTime);
        tv_distance = (TextView) findViewById(R.id.txtDistance);
        tv_address = (TextView) findViewById(R.id.txtAddress);
        circleImageView = (CircleImageView) findViewById(R.id.map_image);
        btn_accept=(Button)findViewById(R.id.btnAccept);
        btn_decline=(Button)findViewById(R.id.btnDecline);
        btn_decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(customerId)){
                    cancleBooking(customerId);
                }
            }
        });
        btn_accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(CustommerCall.this , Mech_Traking.class);
                intent.putExtra("lat" , lat);
                intent.putExtra("lng" , lng);
                startActivity(intent);
                finish();
            }
        });


        //Media player init
        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        if (getIntent() != null) {
            lat = getIntent().getDoubleExtra("lat", -1.0);
            lng = getIntent().getDoubleExtra("lng", -1.0);
            customerId=getIntent().getStringExtra("customer");

            //Just Copy getDirection from Welcom Activity
            getDirection(lat, lng);
        }

    }

    private void cancleBooking(String customerId) {
        Token token=new Token(customerId);
        Notification notification=new Notification("Notice","Mechanic has cancelled your request");
        Sender sender=new Sender(token.getToken(),notification);

        mFCMService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success==1){
                    Toast.makeText(CustommerCall.this,"Cancelled",Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    private void getDirection(double lat, double lng) {
        String reuestApi = null;
        Toast.makeText(getApplication(), "done", Toast.LENGTH_LONG).show();
        try {
            reuestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + Common.mLastLocation.getLatitude() + "," + Common.mLastLocation.getLongitude() + "&" +
                    "destination=" + lat + "," + lng + "&" +
                    "key=" + getResources().getString(R.string.google_directions_api);
            Log.d("Umair", reuestApi);
            Toast.makeText(getApplication(), "done", Toast.LENGTH_LONG).show();
            mService.getPath(reuestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray routes = jsonObject.getJSONArray("routes");

                        //after getting routes, get get first element of routs
                        JSONObject object=routes.getJSONObject(0);
                        //after getting first element  we need get array with name "legs"
                        JSONArray legs=object.getJSONArray("legs");
                        //and get first element of leg array
                        JSONObject legsObject=legs.getJSONObject(0);
                        //Now get Distance
                        JSONObject distance=legsObject.getJSONObject("distance");
                        tv_distance.setText(distance.getString("text"));

                        //Now get Time
                        JSONObject time=legsObject.getJSONObject("duration");
                        tv_time.setText(time.getString("text"));

                        //Now Address
                        String address=legsObject.getString("end_address");
                        tv_address.setText(address);

                    } catch (JSONException e) {

                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(CustommerCall.this, "error" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        mediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onPause() {
        mediaPlayer.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mediaPlayer.start();
        super.onResume();
    }
}
