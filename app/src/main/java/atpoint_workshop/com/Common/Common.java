package atpoint_workshop.com.Common;

import android.location.Location;

import atpoint_workshop.com.Remote.FCMClient;
import atpoint_workshop.com.Remote.IFCMService;
import atpoint_workshop.com.Remote.IGoogleAPI;
import atpoint_workshop.com.Remote.RetrofitClient;

/**
 * Created by ah_abdelhak on 2/28/2018.
 */
public class Common {

    //firebase tables
    public static final String mechanic_tbl="Mechanic";// store all the information of Drivers locations
    public static final String user_Mechanic_tbl="MechanicInformation";//store all the info of drivers who registered
    public static final String user_driver_tbl="DriverInformation";//store all the info of riders who registered
    public static final String pickup_request_tbl="PickupRequest";//store information about pickup Request of user


    public static final String token_table="Tokens";

    public static Location mLastLocation=null;

    public static final String baseURL="https://maps.googleapis.com";
    public static final String fcmURL="https://fcm.googleapis.com/";

    public static IGoogleAPI getGoogleAPI(){
        return RetrofitClient.getClent(baseURL).create(IGoogleAPI.class);
    }

    public static IFCMService getFcmService(){
        return FCMClient.getClent(fcmURL).create(IFCMService.class);
    }
}