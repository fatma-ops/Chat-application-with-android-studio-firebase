package hcmute.edu.vn.thanh0456.zaloclone.network;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

// Créer et utiliser des API Google pour envoyer des notifications ou des données à d’autres appareils en fonction de FCM_TOKEN
public class APIClient {

    private static Retrofit retrofit = null;

    public static  Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://fcm.googleapis.com/fcm/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
