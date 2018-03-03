package atpoint_workshop.com.Remote;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by ah_abdelhak on 2/28/2018.
 */
public class RetrofitClient {
    private static Retrofit retrofit=null;
    public static Retrofit getClent(String baseUrl){
        if (retrofit==null){
            retrofit=new Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

