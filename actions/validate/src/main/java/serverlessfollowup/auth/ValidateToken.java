package serverlessfollowup.auth;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Base64;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.FixedClock;

/**
 * Validates authorization header. Used as first action in sequences to protect
 * them.
 */
public class ValidateToken {

  private static JsonParser gsonParser = new JsonParser();
  private static Gson gson = new Gson();

  /**
   * Input:
   * <ul>
   * <li>services.appid.url - the App ID service instance OAuth url</li>
   * <li>services.appid.clientId - the App ID clientId</li>
   * <li>services.appid.secret - the App ID secret</li>
   * <li>__ow_headers - to decode and validate the Authorization header</li>
   * <li>__ow_headers.authorization - Bearer accessToken [idToken]
   * </ul>
   * 
   * Output:
   * <ul>
   * <li>all input arguments</li>
   * <li>_accessToken: decoded JSON representation of the access token</li>
   * <li>_idToken: decoded JSON representation of the id token if specified in
   * the Authorization header</li>
   * </ul>
   */
  public static JsonObject main(JsonObject args) throws Exception {
    // we expect the Authorization header to be set
    JsonObject headers = args.getAsJsonObject("__ow_headers");
    JsonPrimitive authorization = headers.getAsJsonPrimitive("authorization");
    if (authorization == null) {
      throw new IllegalArgumentException("Authorization header is missing");
    }

    // and to be well-formed
    String authorizationHeader = authorization.getAsString();
    String[] authorizationElements = authorizationHeader.split(" ");
    if (authorizationElements.length < 2 || authorizationElements.length > 3) {
      throw new IllegalArgumentException("Authorization header is malformed");
    }
    if (!authorizationElements[0].equalsIgnoreCase("Bearer")) {
      throw new IllegalArgumentException("Expected Bearer information in Authorization header");
    }

    String accessToken = authorizationElements[1].trim();
    String idToken = authorizationElements.length == 3 ? authorizationElements[2] : null;

    System.out.println("Access token is " + accessToken);
    if (idToken == null) {
      System.out.println("No id token specified");
    } else {
      System.out.println("Id token is " + idToken);
    }

    // use the introspect endpoint to validate the token signature and expiration dates
    String introspectEndpoint = args.getAsJsonPrimitive("services.appid.url").getAsString() + "/introspect";
    String introspectEndpointAuth = Base64.getEncoder()
        .encodeToString((args.getAsJsonPrimitive("services.appid.clientId").getAsString() + ":"
            + args.getAsJsonPrimitive("services.appid.secret").getAsString()).getBytes(StandardCharsets.UTF_8));

    if (!validateToken(introspectEndpoint, introspectEndpointAuth, accessToken)) {
      throw new IllegalArgumentException("Invalid access token");
    }

    if (idToken != null && !validateToken(introspectEndpoint, introspectEndpointAuth, idToken)) {
      throw new IllegalArgumentException("Invalid id token");
    }

    // create JSON representation of the token that we can use in subsequent actions
    JwtParser parser = Jwts.parser();
    parser.setClock(new FixedClock(DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK).parse("11/1/2017")));
    Jwt<?, Claims> parsedAccessToken;
    Jwt<?, Claims> parsedIdToken;
    try {
      // we remove the last part of the token as we have already verified the
      // signature by calling the introspect endpoint
      // another option would have been to retrieve the /publickey for this
      // tenant and to validate the signature on our own
      parsedAccessToken = parser.parse(accessToken.substring(0, accessToken.lastIndexOf('.') + 1));
      parsedIdToken = idToken == null ? null : parser.parse(idToken.substring(0, idToken.lastIndexOf('.') + 1));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }

    // inject decoded versions of the token in the message so next action does
    // not need to do this
    args.add("_accessToken", gson.toJsonTree(parsedAccessToken.getBody()));
    if (parsedIdToken != null) {
      args.add("_idToken", gson.toJsonTree(parsedIdToken.getBody()));
    }

    // valid token detected, let it pass
    return args;
  }

  /**
   * Validates token with the App ID API https://appid-oauth.ng.bluemix.net/swagger-ui/#!/Authorization_Server_V3/introspect
   */
  private static boolean validateToken(String introspectEndpoint, String introspectEndpointAuth, String token) {
    try {
      // pass the token as encoded form
      String urlParameters = "token=" + URLEncoder.encode(token, "UTF-8");
      byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

      URL url = new URL(introspectEndpoint);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setInstanceFollowRedirects(false);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Accept", "application/json");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
      conn.setRequestProperty("charset", "utf-8");
      conn.setRequestProperty("WWW-Authenticate", "Basic realm=\"token\"");
      conn.setRequestProperty("Authorization", "Basic " + introspectEndpointAuth);
      conn.setUseCaches(false);

      DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
      wr.write(postData);

      JsonObject response = (JsonObject) gsonParser
          .parse(new BufferedReader(new InputStreamReader(conn.getInputStream())));

      System.out.println("Introspect returned " + response);
      return response.has("active") && response.getAsJsonPrimitive("active").getAsBoolean();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}
