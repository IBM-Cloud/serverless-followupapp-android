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

/**
 * Stores a feedback in the database.
 */
public class AddFeedback {

  /**
   * Input:
   * <ul>
   *   <li>services.cloudant.url - Cloudant service url including username and password
   *   <li>_accessToken - the decoded JSON representation of the accessToken</li>
   *   <li>message - the message to store</li>
   * </ul>
   * 
   * Output:
   * <ul>
   *   <li>body.ok: true|false</li>
   *   <li>body.response: details from the database</li>
   * </ul>
   */
  public static JsonObject main(JsonObject args) throws Exception {
    // read the tokens
    JsonObject accessToken = (JsonObject) args.get("_accessToken");
    
    // if no access token, return error
    if (accessToken == null) {
      throw new IllegalArgumentException("Missing access token");
    }

    // extract the subject from the token
    String subject = accessToken.getAsJsonPrimitive("sub").getAsString();

    // store the feedback
    CloudantClient client = ClientBuilder.url(new URL(args.getAsJsonPrimitive("services.cloudant.url").getAsString()))
        .build();
    Database feedbacks = client.database("feedback", true);

    Map<String, Object> newFeedback = new LinkedHashMap<String, Object>();
    newFeedback.put("subject", subject);
    newFeedback.put("message", args.getAsJsonPrimitive("message").getAsString());
    Response newFeedbackResponse = feedbacks.save(newFeedback);

    JsonObject status = new JsonObject();
    status.addProperty("ok", newFeedbackResponse.getError() == null);
    status.add("response", new Gson().toJsonTree(newFeedbackResponse));
    
    JsonObject body = new JsonObject();
    body.add("body", status);
    return body;
  }

}
