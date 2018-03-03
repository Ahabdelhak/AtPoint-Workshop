package atpoint_workshop.com.Services;

import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import atpoint_workshop.com.CustommerCall;

/**
 * Created by ah_abdelhak on 2/28/2018.
 */
public class MyFirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        //Because we will send the firebase message with conatin Lat and lang from rider app
        //So we will  convert message to  LatLng
        LatLng customer_location=new Gson().fromJson(remoteMessage.getNotification().getBody(),LatLng.class);

        Intent intent=new Intent(getBaseContext(), CustommerCall.class);
        intent.putExtra("lat",customer_location.latitude);
        intent.putExtra("lng",customer_location.longitude);
        intent.putExtra("customer",remoteMessage.getNotification().getTitle());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
