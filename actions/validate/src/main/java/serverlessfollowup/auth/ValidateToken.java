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
import java.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

public class ValidateToken {

  static JwtParser makeParser(String algo, String modulus, String exponent) {
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

    // // get the app id public key to verify the token
    JsonObject publicKey = null;
    try {
      URL publickeyUrl = new URL(args.getAsJsonPrimitive("services.appid.url").getAsString() + "/publickey");
      HttpURLConnection connection = (HttpURLConnection) publickeyUrl.openConnection();

      JsonParser gsonParser = new JsonParser();
      publicKey = (JsonObject) gsonParser.parse(new BufferedReader(new InputStreamReader(connection.getInputStream())));
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not retrieve public key", e);
    }

    String algo = publicKey.getAsJsonPrimitive("kty").getAsString();
    String modulus = publicKey.getAsJsonPrimitive("n").getAsString();
    String exponent = publicKey.getAsJsonPrimitive("e").getAsString();
    System.out.println("Public key is " + algo + ", " + modulus + ", " + exponent);

    JwtParser parser = makeParser(algo, modulus, exponent);
    try {
      parser.parse(accessToken);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }

    // valid token detected, let it pass
    return args;
  }

}
