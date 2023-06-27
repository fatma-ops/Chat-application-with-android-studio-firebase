package hcmute.edu.vn.thanh0456.zaloclone.network;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

// Interface pour définir la structure du contenu des messages et des notifications
//pour envoyer vers un autre appareil
public interface APIService {

    @POST("send")
    Call<String> sendMessage(
            @HeaderMap HashMap<String, String> headers,
            @Body String messageBody
    );
}
