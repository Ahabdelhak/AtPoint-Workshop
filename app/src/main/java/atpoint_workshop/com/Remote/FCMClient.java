package atpoint_workshop.com.Remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by ah_abdelhak on 2/28/2018.
 */
public class FCMClient {
    private static Retrofit retrofit=null;
    public static Retrofit getClent(String baseUrl){
        if (retrofit==null){
            retrofit=new Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

