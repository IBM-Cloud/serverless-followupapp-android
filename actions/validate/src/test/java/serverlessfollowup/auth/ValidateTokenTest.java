package serverlessfollowup.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.DateFormat;
import java.util.Locale;

import org.junit.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.impl.FixedClock;

public class ValidateTokenTest {

  @Test
  public void testJWK() throws Exception {
    JwtParser parser = Jwts.parser();

    try {
      parser.parse("123");
      assertNull("should not be reached");
    } catch (MalformedJwtException e) {
    } catch (Exception e) {
      assertNull("should not be reached");
    }
    
    String accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJhcHBJZC0xNTA0Njg1OTYxMDAwIn0.eyJpc3MiOiJhcHBpZC1vYXV0aC5uZy5ibHVlbWl4Lm5ldCIsImV4cCI6MTUxMDI0MTQ1MSwiYXVkIjoiMjM5MTIwZDNhZjBmNDdiMDgzNDZkZWQ4NGI5ZTM4ZGFhYTI1ZGIwNyIsInN1YiI6ImYyMWJkOWJhLWRkMWItNDkxYi04NDhmLTU1YTBlOGNhNjQxNiIsImFtciI6WyJmYWNlYm9vayJdLCJpYXQiOjE1MTAyMzc4NTEsInRlbmFudCI6IjQzYWU4NjM1LTdiZTQtNDg2Yy1hNjc5LTJlYmZjZjU0ODAyMyIsInNjb3BlIjoiYXBwaWRfZGVmYXVsdCBhcHBpZF9yZWFkcHJvZmlsZSBhcHBpZF9yZWFkdXNlcmF0dHIgYXBwaWRfd3JpdGV1c2VyYXR0ciJ9.CP8xvX6OkIxeGE1PZXt9E6zeyT3bujmeSO4Vm9CWzrtDntM8b5nE1q8AtM9kNsJyeGuoogyVbbg7igIKroJId4OIv4px_Ypy8_YySOx-3DGFE2AgosxQb1nSeuHWDMETZLR4KEj6HZ9XDaVAOtXeL4hbrwFokjqbtocI5wnxmnfov33wjkMPCPsDwxXLvQpdSYdRXGADg7CW6ufekW9SVw41a5gm8Qc566y1NIiQ2HD_huroHM0HJZ5Ces1JzD0lWB5lsBd3Dz6oIiPntiF5WrFLHZrEgNegrOYq7IB-l_K1Qq3zajqfV9rkuGvQbG64wjOWtMvsvaZ09vvocuFo_A";
    try {
      parser.parse(accessToken);
      assertNull("should not be reached");
    } catch (IllegalArgumentException e) {
      // expected as the token is signed but we don't verify signatures
    } catch (Exception e) {
      assertNull("should not be reached");
    }
    
    // remove the signature from the accessToken
    accessToken = accessToken.substring(0, accessToken.lastIndexOf('.') + 1);
    
    Jwt<?, ?> token = parser.setClock(new FixedClock(DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK).parse("11/1/2017"))).parse(accessToken);
    Claims body = (Claims)token.getBody();
    assertEquals("f21bd9ba-dd1b-491b-848f-55a0e8ca6416", body.get("sub"));
    System.out.println(body);
  }

}