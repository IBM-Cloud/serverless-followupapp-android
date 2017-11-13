package serverlessfollowup.users;

import com.google.gson.JsonObject;

public class NotifyUser {

  public static JsonObject main(JsonObject args) throws Exception {    
    JsonObject response = new JsonObject();
    response.addProperty("ok", true);
    return response;
  }
  
}
