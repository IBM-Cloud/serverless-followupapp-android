package serverlessfollowup.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
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
 * Validates authorization header. Used as first action in sequences to protect them.
 */
public class ValidateToken {

  private static JsonParser gsonParser = new JsonParser();
  private static Gson gson = new Gson();

  /**
   * Input:
   * <ul>
   *   <li>__ow_headers - to decode and validate the Authorization header</li>
   *   <li>__ow_headers.authorization - Bearer accessToken [idToken]
   * </ul>
   * 
   * Output:
   * <ul>
   *   <li>all input arguments</li>
   *   <li>_accessToken: decoded JSON representation of the access token</li>
   *   <li>_idToken: decoded JSON representation of the id token if specified in the Authorization header</li>
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
    
    // // get the app id public key to verify the token
    JsonObject publicKey = null;
    try {
      URL publickeyUrl = new URL(args.getAsJsonPrimitive("services.appid.url").getAsString() + "/publickey");
      HttpURLConnection connection = (HttpURLConnection) publickeyUrl.openConnection();

      publicKey = (JsonObject) gsonParser.parse(new BufferedReader(new InputStreamReader(connection.getInputStream())));
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not retrieve public key", e);
    }

    String algo = publicKey.getAsJsonPrimitive("kty").getAsString();
    String modulus = publicKey.getAsJsonPrimitive("n").getAsString();
    String exponent = publicKey.getAsJsonPrimitive("e").getAsString();
    System.out.println("Public key is " + algo + ", " + modulus + ", " + exponent);

    JwtParser parser = makeParser(algo, modulus, exponent);
    Jwt<?, Claims> parsedAccessToken;
    Jwt<?, Claims> parsedIdToken;
    try {
      parsedAccessToken = parser.parse(accessToken);
      parsedIdToken = idToken == null ? null : parser.parse(idToken);
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

  private static JwtParser makeParser(String algo, String modulus, String exponent) {
    if (!"RSA".equals(algo)) {
      throw new IllegalArgumentException(algo + " is not a supported algorithm");
    }

    RSAPublicKeySpec rsaKeySpec = new RSAPublicKeySpec(new BigInteger(Base64.getDecoder().decode(modulus)),
        new BigInteger(Base64.getDecoder().decode(exponent)));

    KeyFactory keyF;
    try {
      keyF = KeyFactory.getInstance(algo);
      PublicKey publicKey = keyF.generatePublic(rsaKeySpec);
      return Jwts.parser().setSigningKey(publicKey);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    } catch (InvalidKeySpecException e) {
      throw new IllegalArgumentException(e);
    }
  }

}
