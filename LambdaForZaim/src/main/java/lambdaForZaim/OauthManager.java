package lambdaForZaim;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import java.util.HashMap;
import java.util.Map;
import lambdaForZaim.model.ScribeZaimApi;

public class OauthManager {
  private String consumerKey = null;
  private String consumerSecret = null;
  private String accessToken = null;
  private String accessTokenSecret = null;

  private static final String callbackURL = "https://www.zaim.net";
  private OAuth10aService service;
  private OAuth1AccessToken oauth1AccessToken;

  public OauthManager() {
    consumerKey = System.getenv("CONSUMER_KEY");
    consumerSecret = System.getenv("CONSUMER_SECRET");
    accessToken = System.getenv("ACCESS_TOKEN");
    accessTokenSecret = System.getenv("ACCESS_TOKEN_SECRET");
  }

  public OAuth1AccessToken getOauth1AccessToken() {
    return oauth1AccessToken;
  }

  public OAuth10aService getService() {
    return service;
  }

  public Map<String, String> getAccessTokens() {
    if (accessToken == null || accessTokenSecret == null) {
      oauthSign();
    }
    return new HashMap<>() {{
      put("AccessToken", accessToken);
      put("AccessTokenSecret", accessTokenSecret);
    }};
  }

  public void oauthSign() {
    if (consumerKey == null || consumerSecret == null) {
      System.err.println("consumerKey or/and consumerSecret are `null`");
      System.exit(-1);
    } else if (accessToken == null || accessTokenSecret == null) {
      System.err.println("accessToken or/and accessTokenSecret are `null`");
      System.exit(-2);
    }
    service = new ServiceBuilder(consumerKey)
        .apiSecret(consumerSecret)
        .callback(callbackURL)
        .build(ScribeZaimApi.instance());
    oauth1AccessToken = new OAuth1AccessToken(accessToken, accessTokenSecret);
  }

}
