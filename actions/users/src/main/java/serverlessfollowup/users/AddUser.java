package serverlessfollowup.users;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Adds a new user.
 */
public class AddUser {

  /**
   * Input:
   * <ul>
   *   <li>services.cloudant.url - Cloudant service url including username and password
   *   <li>_accessToken - the decoded JSON representation of the access token</li>
   *   <li>_idToken - the decoded JSON representation of the id token</li>
   *   <li>deviceId - the mobile device id of the user, used to send notifications back to the device
   * </ul>
   * 
   * Output:
   * <ul>
   *   <li>body.ok: true|false</li>
   *   <li>body.response: optional details from the database</li>
   * </ul>
   */
  public static JsonObject main(JsonObject args) throws Exception {
    // read the tokens
    JsonObject accessToken = (JsonObject) args.get("_accessToken");
    JsonObject idToken = (JsonObject) args.get("_idToken");
    String deviceId = args.getAsJsonPrimitive("deviceId").getAsString();
    
    // if one is missing, return error
    if (accessToken == null) {
      throw new IllegalArgumentException("Missing access token");
    }

    if (idToken == null) {
      throw new IllegalArgumentException("Missing id token");
    }

    if (deviceId == null) {
      throw new IllegalArgumentException("Missing deviceId");
    }
    
    // extract the subject
    String subject = accessToken.getAsJsonPrimitive("sub").getAsString();

    // look for an existing user with this subject
    CloudantClient client = ClientBuilder.url(new URL(args.getAsJsonPrimitive("services.cloudant.url").getAsString()))
        .build();
    Database users = client.database("users", true);

    JsonObject selector = new JsonObject();
    selector.addProperty("subject", subject);
    List<LinkedHashMap> existingUsers = users.findByIndex("\"selector\": " + new Gson().toJson(selector),
        LinkedHashMap.class);

    if (existingUsers.isEmpty()) {
      // if not found, create a new user then return the user
      System.out.println("No existing user found, creating a new one");

      boolean isAnon = "appid_anon".equals(idToken.getAsJsonArray("amr").get(0).getAsString());
      
      Map<String, Object> newUser = new LinkedHashMap<String, Object>();
      newUser.put("subject", subject);
      newUser.put("deviceId", deviceId);
      if (isAnon) {
        System.out.println("This is an anonymous user");
        newUser.put("name", "Guest");
      } else {
        newUser.put("name", idToken.getAsJsonPrimitive("name").getAsString());
        newUser.put("email", idToken.getAsJsonPrimitive("email").getAsString());
        newUser.put("picture", idToken.getAsJsonPrimitive("picture").getAsString());
      }
      newUser.put("accessToken", accessToken);
      newUser.put("idToken", idToken);

      // save the user
      Response newUserResponse = users.save(newUser);

      JsonObject status = new JsonObject();
      status.addProperty("ok", newUserResponse.getError() == null);
      status.add("response", new Gson().toJsonTree(newUserResponse));

      JsonObject body = new JsonObject();
      body.add("body", status);
      return body;
    } else {
      // if found, we're good
      System.out.println("User is already registered");

      Map<String, Object> existingUser = existingUsers.get(0);
      System.out.println(new Gson().toJson(existingUser));
      
      JsonObject status = new JsonObject();
      status.addProperty("ok", true);

      JsonObject body = new JsonObject();
      body.add("body", status);
      return body;
    }
  }
}
