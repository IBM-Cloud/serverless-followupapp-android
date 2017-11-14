package serverlessfollowup.feedback;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AddComplaint {

  public static JsonObject main(JsonObject args) throws Exception {
    // read the tokens
    JsonObject accessToken = (JsonObject) args.get("_accessToken");
    
    // if no access token, return error
    if (accessToken == null) {
      throw new IllegalArgumentException("Missing access token");
    }

    // extract the subject from the token
    String subject = accessToken.getAsJsonPrimitive("sub").getAsString();

    // store the complaints
    CloudantClient client = ClientBuilder.url(new URL(args.getAsJsonPrimitive("services.cloudant.url").getAsString()))
        .build();
    Database complaints = client.database("complaints", true);

    Map<String, Object> newComplaint = new LinkedHashMap<String, Object>();
    newComplaint.put("subject", subject);
    newComplaint.put("message", args.getAsJsonPrimitive("message").getAsString());
    Response newComplaintResponse = complaints.save(newComplaint);

    JsonObject status = new JsonObject();
    status.addProperty("ok", newComplaintResponse.getError() == null);
    status.add("response", new Gson().toJsonTree(newComplaintResponse));
    
    JsonObject body = new JsonObject();
    body.add("body", status);
    return body;
  }

}
