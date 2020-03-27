package zaimtoslack;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.lambda.model.ServiceException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zaimtoslack.model.GatewayRequest;
import zaimtoslack.model.GatewayResponse;
import zaimtoslack.model.SlackRequest;
import zaimtoslack.model.ZaimResponse;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<GatewayRequest, GatewayResponse> {
  private static ObjectMapper objectMapper = new ObjectMapper();
  private final String SLACK_BOT_USER_ACCESS_TOKEN = System.getenv("SLACK_BOT_USER_ACCESS_TOKEN");
  private final String SLACK_APP_AUTH_TOKEN = System.getenv("SLACK_APP_AUTH_TOKEN");
  private final String SLACK_USER_ID = System.getenv("SLACK_USER_ID");
  private final String SLACK_USER_NAME = System.getenv("SLACK_USER_NAME");
  private final String SLACK_CHANNEL_ID = System.getenv("SLACK_CHANNEL_ID");
  private final String SLACK_POST_URL = "https://slack.com/api/chat.postMessage";

  public GatewayResponse handleRequest(final GatewayRequest input, final Context context) {
    // TODO: input インスタンスを上手く読み込めていない．jsonオブジェクトから変換しているもの
    // objectMapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    // objectMapper.configure(Feature.IGNORE_UNDEFINED, true);
    Map<String, String> headers = new HashMap<>();
    String retText = "";
    String eventText;

    headers.put("Content-Type", "application/json");
    headers.put("X-Custom-Header", "application/json");

    // auth slack api
    if (input.getBody().contains("\"challenge\"")) {
      try{
        Map<String, String> map = objectMapper.readValue(
            objectMapper.writeValueAsString(input.getBody()),
            new TypeReference<HashMap<String, String>>(){}
        );
        if (map.containsKey("challenge")) {
          return new GatewayResponse("{\"challenge\": \"" + map.get("challenge").toString() + "\"}", headers, 200);
        } else if (! map.containsKey("event")) {
          return new GatewayResponse("no contain `event` key", headers, 200);
        }
      } catch (Exception e) {
        return postErrorToSlackAndHandleRequest(e, "auth error", headers);
      }
    }

    // SlackRequest slackRequest;
    try{
      // Map<String, Object> aaa = objectMapper.readValue(input.getBody(), new TypeReference<HashMap<String, Object>>(){});
      // System.out.println(aaa.get("event").toString());
      var slackRequest = objectMapper.readValue(
          objectMapper.writeValueAsString(input.getBody()),
          SlackRequest.class
      );
      System.out.println(slackRequest);
      eventText = slackRequest.getEvent().getText();
      if (slackRequest.getEvent().getUser().equals(SLACK_USER_ID)) {
        slackPost("speak by me");
        return new GatewayResponse("speak by me", headers, 200);
      }
    } catch (Exception e) {
      return postErrorToSlackAndHandleRequest(e, "slack request error", headers);
    }

    try {
      slackPost(
          "AWS_ACCESS_KEY_ID: " + System.getenv("AWS_ACCESS_KEY_ID") + "\n"
              + "AWS_SECRET_ACCESS_KEY: " + System.getenv("AWS_SECRET_ACCESS_KEY"));
      AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
          .withCredentials(new EnvironmentVariableCredentialsProvider())
          .withRegion(Regions.AP_NORTHEAST_1).build();
      InvokeRequest invokeRequest = new InvokeRequest()
          .withFunctionName(getFunctionName(awsLambda))
          .withPayload(getPayload(eventText));
      InvokeResult invokeResult = awsLambda.invoke(invokeRequest);
      ZaimResponse zaimResponse = objectMapper.readValue(
          new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8),
          ZaimResponse.class
      );
      var totalString = zaimResponse.getTotalString();
      Map<String, Integer> amountForCategory = zaimResponse.getAmountForCategory();
      retText += totalString;
      for(Map.Entry<String, Integer> amount: amountForCategory.entrySet()) {
        retText += "\n" + amount.getKey() + ":  \t" + amount.getValue();
      }
      //write out the return value
    } catch (IOException e) {
      return postErrorToSlackAndHandleRequest(e, "IO error", headers);
    }
    slackPost(retText);
    return new GatewayResponse(retText, headers, 200);
  }

  private String getPayload(String eventText) {
    int n = 1;
    boolean amountOnly;
    String period;

    // 出費を表す単語が含まれていたら出費のみ
    var amountPattern = Pattern.compile("(支出|出費|費用|消費|出勤)");
    amountOnly = amountPattern.matcher(eventText).find();

    // `\d + 日` ならn日分の費用に
    var periodPattern = Pattern.compile("\\d{1,2}日");
    period = periodPattern.matcher(eventText).find() ? "day" : "month";

    // 何 月|日 分の情報を見るかを推定
    var nPattern = Pattern.compile("\\d{1,2}月|日");
    Matcher matcher = nPattern.matcher(eventText);
    if (matcher.find()) {
      var nRep = eventText.substring(matcher.start(), matcher.end());
      nRep.replaceAll("月", "").replaceAll("日", "");
      if (nRep.matches("\\d+")) {
        n = Integer.parseInt(nRep);
      }
    }

    return "{\"queryStringParameters\":"
        + "{\"n\": " + n + ","
        + "\"amountOnly\": " + amountOnly + ","
        + "\"period\": \"" + period + "\"}}";
  }

  private String getFunctionName(AWSLambda awsLambda) {
    try {
      ListFunctionsResult results = awsLambda.listFunctions();
      List<FunctionConfiguration> listFuncConfigs = results.getFunctions();
      for (Iterator<FunctionConfiguration> iter = listFuncConfigs.iterator(); iter.hasNext();) {
        FunctionConfiguration config = iter.next();
        if (config.getFunctionName().contains("LambdaForZaim")) {
          return config.getFunctionName();
        }
      }
    } catch (ServiceException e) {
      slackPost(e.toString());
      e.getStackTrace();
    } catch (AWSLambdaException e) {
      slackPost(e.toString());
      e.getStackTrace();
    }
    return "";
  }

  private void slackPost(String text) {
    var body =  "{"
        + "\"channel\": \"" + SLACK_CHANNEL_ID + "\","
        + "\"text\": \"" + text + "\""
        + "}";
    try {
      HttpClient client = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder(URI.create(SLACK_POST_URL))
                                       .setHeader("Content-Type", "application/json")
                                       .setHeader("Authorization", "Bearer " + SLACK_BOT_USER_ACCESS_TOKEN)
                                       .POST(BodyPublishers.ofString(body))
                                       .build();
      HttpResponse response = client.send(request, BodyHandlers.ofString());
    } catch (IOException e) {
      System.err.println(e.getStackTrace().toString());
      System.exit(-1);
    } catch (InterruptedException e) {
      System.err.println(e.getStackTrace().toString());
      System.exit(-1);
    }
  }

  private GatewayResponse postErrorToSlackAndHandleRequest(Exception e, String errorName, Map<String, String> headers) {
    String tmpStr = "";
    for (var tmp: e.getStackTrace()) {
      tmpStr += tmp + "\n";
    }
    errorName += errorName.length() > 0 ? ": \n" : "";
    slackPost(errorName + tmpStr);
    return new GatewayResponse(errorName + tmpStr, headers, 200);
  }
}
