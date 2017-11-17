package serverlessfollowup.users;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Prepares a notification for a user.
 */
public class PrepareUserNotification {

  /**
   * Input:
   * <ul>
   *   <li>services.cloudant.url - Cloudant service url including username and password
   *   <li>subject - the user to notify</li>
   *   <li>message - the message to send</li>
   * </ul>
   * 
   * Output:
   * <ul>
   *   <li>deviceIds - an array containing the user deviceId</li>
   *   <li>text - the actual message to send where references to <code>{{name}}</code> from the original message have been replaced with the actual user name</li>
   * </ul>
   */
  public static JsonObject main(JsonObject args) throws Exception {    
    
    String subject = args.getAsJsonPrimitive("subject").getAsString();
    String notificationText = args.getAsJsonPrimitive("message").getAsString();

    // find the related user
    CloudantClient client = ClientBuilder.url(new URL(args.getAsJsonPrimitive("services.cloudant.url").getAsString()))
        .build();
    Database users = client.database("users", true);
    
    JsonObject selector = new JsonObject();
    selector.addProperty("subject", subject);
    List<LinkedHashMap> existingUsers = users.findByIndex("\"selector\": " + new Gson().toJson(selector),
        LinkedHashMap.class);

    if (existingUsers.isEmpty()) {
      throw new IllegalArgumentException("No such user");
    }
    
    Map<String, Object> existingUser = existingUsers.get(0);
    System.out.println(new Gson().toJson(existingUser));
    
    // prepare the text for the notification
    notificationText = notificationText.replace("{{name}}", (String)existingUser.get("name"));
    
    JsonObject response = new JsonObject();
    JsonArray deviceIds = new JsonArray();
    deviceIds.add((String)existingUser.get("deviceId"));
    response.add("deviceIds", deviceIds);
    response.addProperty("text", notificationText);
    return response;
  }
  
}
