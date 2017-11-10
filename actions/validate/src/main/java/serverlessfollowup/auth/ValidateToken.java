package serverlessfollowup.auth;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.json.JSONObject;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

public class ValidateToken {

  static JwtParser makeParser(String algo, String modulus, String exponent) {
    if (!"RSA".equals(algo)) {
      throw new IllegalArgumentException(algo + " is not a supported algorithm");
    }
    
    RSAPublicKeySpec rsaKeySpec = new RSAPublicKeySpec(
        new BigInteger(Base64.getDecoder().decode(modulus)),
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
  
  public static JsonObject main(JsonObject args) {


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
    System.out.println("Id token is " + idToken);

    // get the app id public key
    JSONObject publicKey = null;
    try {
      publicKey = Unirest.get(args.getAsJsonPrimitive("services.appid.url").getAsString() + "/publickey").asJson().getBody().getObject();
    } catch (UnirestException e) {
      throw new IllegalArgumentException("Could not retrieve public key", e);
    }
    
    JwtParser parser = makeParser(publicKey.getString("kty"), publicKey.getString("n"), publicKey.getString("e"));
    try {
      parser.parse(accessToken);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }
    
    // valid token detected, let it pass
    return args;
  }

}
