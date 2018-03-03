package atpoint_workshop.com.Remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by ah_abdelhak on 2/28/2018.
 */
public interface IGoogleAPI {
    @GET
    Call<String> getPath(@Url String url);
}
