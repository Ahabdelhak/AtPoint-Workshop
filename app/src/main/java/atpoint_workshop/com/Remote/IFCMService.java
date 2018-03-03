package atpoint_workshop.com.Remote;

import atpoint_workshop.com.Model.FCMResponse;
import atpoint_workshop.com.Model.Sender;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by ah_abdelhak on 2/28/2018.
 */
public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAydm6Gag:APA91bGSvtME0KfnO6ERsuZSK2k4QV0zyWF2Jgm4zjyqjfbUsd9aHyJaQg-DWboDR_xCMLGtcVPXJgrpVyeNEeUcPKjJ331ADR6SWk5F97FYI4znLKeleV7xVp8E2lr5TvXyGkU2Enz-"
    })
    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body Sender body);
}

