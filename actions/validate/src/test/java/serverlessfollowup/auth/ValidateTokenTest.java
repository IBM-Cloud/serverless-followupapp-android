package serverlessfollowup.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.DateFormat;
import java.util.Locale;

import org.junit.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.FixedClock;

public class ValidateTokenTest {

  @Test
  public void testJWK() throws Exception {

    String key_kty = "RSA";
    String key_n = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDH+wPrKYG1KVlzQUVtBghR8n9dzcShSZo0+3KgyVdOea7Ei7vQ1U4wRn1zlI5rSqHDzFitblmqnB2anzVvdQxLQ3UqEBKBfMihnLgCSW8Xf7MCH+DSGHNvBg2xSNhcfEmnbLPLnbuz4ySn1UB0lH2eqxy5" + "0zstxhTY0binD9Y+rwIDAQAB";
    key_n = "AJ+E8O4KJT6So/lUkCIkU0QKW7QjMp9vG7S7vZx0M399idZ4mP7iWWW6OTvjLHpDTx7uapiwRQktDNx3GHigJDmbbu8/VtS5K6J6be1gVrvu6pxmZtrz8PazlH5WYxkuUIfUYpzyfUubZzqzuVWqQO0W9kOhFN7HILAxb1WsQREX+iLg14MGGafrQnJgXHBAwSH0OOJr7v+nRz8AFCAicN8v0uIar9lRA7JRHQCZtpI/lkSGKKBQT1Zae9+9YlWbZlfXErQS1uYoAb3j3uaLbJVO7SNjQqEsRTjYxfpBsTtkvJmwcwA0wV2gBO3JR6K6ep0Y/KyMR8w9Fd/lvJqdltU=";
    String key_e = "AQAB";

    JwtParser parser = ValidateToken.makeParser(key_kty, key_n, key_e);
    assertNotNull(parser);

    try {
      parser.parse("123");
      assertNull("should not be reached");
    } catch (MalformedJwtException e) {
    } catch (Exception e) {
      assertNull("should not be reached");
    }
    
    try {
      parser.parse("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.EkN-DOsnsuRjRO6BxXemmJDm3HbxrbRzXglbN2S4sOkopdU4IsDxTI8jO19W_A4K8ZPJijNLis4EZsHeY559a4DFOd50_OqgHGuERTqYZyuhtF39yxJPAjUESwxk2J5k_4zM3O-vtd1Ghyo4IbqKKSy6J9mTniYJPenn5-HIirE");
    } catch (MalformedJwtException e) {
      assertNull("should not be reached");
    } catch (SignatureException e) {
      // expected as this token was not signed with our private key
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("should not be reached");
    }
    
    Jwt<?, ?> token = parser.setClock(new FixedClock(DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK).parse("11/1/2017"))).parse("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJhcHBJZC0xNTA0Njg1OTYxMDAwIn0.eyJpc3MiOiJhcHBpZC1vYXV0aC5uZy5ibHVlbWl4Lm5ldCIsImV4cCI6MTUxMDI0MTQ1MSwiYXVkIjoiMjM5MTIwZDNhZjBmNDdiMDgzNDZkZWQ4NGI5ZTM4ZGFhYTI1ZGIwNyIsInN1YiI6ImYyMWJkOWJhLWRkMWItNDkxYi04NDhmLTU1YTBlOGNhNjQxNiIsImFtciI6WyJmYWNlYm9vayJdLCJpYXQiOjE1MTAyMzc4NTEsInRlbmFudCI6IjQzYWU4NjM1LTdiZTQtNDg2Yy1hNjc5LTJlYmZjZjU0ODAyMyIsInNjb3BlIjoiYXBwaWRfZGVmYXVsdCBhcHBpZF9yZWFkcHJvZmlsZSBhcHBpZF9yZWFkdXNlcmF0dHIgYXBwaWRfd3JpdGV1c2VyYXR0ciJ9.CP8xvX6OkIxeGE1PZXt9E6zeyT3bujmeSO4Vm9CWzrtDntM8b5nE1q8AtM9kNsJyeGuoogyVbbg7igIKroJId4OIv4px_Ypy8_YySOx-3DGFE2AgosxQb1nSeuHWDMETZLR4KEj6HZ9XDaVAOtXeL4hbrwFokjqbtocI5wnxmnfov33wjkMPCPsDwxXLvQpdSYdRXGADg7CW6ufekW9SVw41a5gm8Qc566y1NIiQ2HD_huroHM0HJZ5Ces1JzD0lWB5lsBd3Dz6oIiPntiF5WrFLHZrEgNegrOYq7IB-l_K1Qq3zajqfV9rkuGvQbG64wjOWtMvsvaZ09vvocuFo_A");
    Claims body = (Claims)token.getBody();
    assertEquals("f21bd9ba-dd1b-491b-848f-55a0e8ca6416", body.get("sub"));
  }

}