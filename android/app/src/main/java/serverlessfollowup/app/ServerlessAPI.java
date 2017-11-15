package serverlessfollowup.app;

import com.google.gson.Gson;
import com.ibm.bluemix.appid.android.api.tokens.AccessToken;
import com.ibm.bluemix.appid.android.api.tokens.IdentityToken;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by fred on 14/11/2017.
 */

public class ServerlessAPI {

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static ServerlessAPI api;

    public static void initialize(String backendUrl) {
        api = new ServerlessAPI(backendUrl);
    }

    public static ServerlessAPI instance() {
        return api;
    }

    private final String backendUrl;

    private final OkHttpClient client = new OkHttpClient();

    private ServerlessAPI(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public void register(AccessToken accessToken, IdentityToken identityToken, String deviceId) throws Exception {
        Map<String, Object> bodyContent = new HashMap<String, Object>();
        bodyContent.put("deviceId", deviceId);
        RequestBody body = RequestBody.create(JSON, new Gson().toJson(bodyContent));
        Request request = new Request.Builder()
                .url(backendUrl + "/users-add-sequence")
                .addHeader("Authorization", "Bearer " + accessToken.getRaw() + " " + identityToken.getRaw())
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
    }

    public void sendFeedback(AccessToken accessToken, String text) throws Exception {
        Map<String, Object> feedback = new HashMap<String, Object>();
        feedback.put("message", text);
        RequestBody body = RequestBody.create(JSON, new Gson().toJson(feedback));
        Request request = new Request.Builder()
                .url(backendUrl + "/feedback-put-sequence")
                .addHeader("Authorization", "Bearer " + accessToken.getRaw())
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
    }
}
