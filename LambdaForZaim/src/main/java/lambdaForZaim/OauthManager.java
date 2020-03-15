package lambdaForZaim;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
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
    Map<String, String> map = new HashMap<>();
    map.put("AccessToken", accessToken);
    map.put("AccessTokenSecret", accessTokenSecret);
    return map;
  }

  public void oauthSign() {
    service = new ServiceBuilder(consumerKey)
        .apiSecret(consumerSecret)
        .callback(callbackURL)
        .build(ScribeZaimApi.instance());
    if (accessToken == null || accessTokenSecret == null) {
      try {
        final OAuth1RequestToken requestToken = service.getRequestToken();
        final String authUrl = service.getAuthorizationUrl(requestToken);
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);

        System.out.println("open this url with your browser");
        System.out.println(authUrl);
        Scanner in = new Scanner(System.in);
        System.out.print("input oauth_verifier >> ");

        oauth1AccessToken = service.getAccessToken(requestToken, in.nextLine());
        accessToken = oauth1AccessToken.getToken();
        accessTokenSecret = oauth1AccessToken.getTokenSecret();
      } catch (ExecutionException e) {
        System.out.println(e);
        System.exit(-1);
      } catch (InterruptedException e) {
        System.out.println(e);
        System.exit(-2);
      } catch (IOException e) {
        System.out.println(e);
        System.exit(-3);
      }
    } else {
        oauth1AccessToken = new OAuth1AccessToken(accessToken, accessTokenSecret);
    }
  }


}
