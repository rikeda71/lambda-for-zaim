package zaimtoslack;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.lambda.model.ServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
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
import zaimtoslack.model.ZaimResponse;


/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<GatewayRequest, GatewayResponse> {
  private static ObjectMapper objectMapper = new ObjectMapper();
  private final String SLACK_BOT_USER_ACCESS_TOKEN = System.getenv("SLACK_BOT_USER_ACCESS_TOKEN");
  private final String SLACK_CHANNEL_ID = System.getenv("SLACK_CHANNEL_ID");
  private final String SLACK_POST_URL = "https://slack.com/api/chat.postMessage";

  public GatewayResponse handleRequest(final GatewayRequest input, final Context context) {
    Map<String, String> headers = new HashMap<>();
    String retText = "";

    headers.put("Content-Type", "application/json");
    headers.put("X-Custom-Header", "application/json");

    // auth slack api
    if (input.getBody().contains("\"challenge\"")) {
      try{
        Map<String, String> map = objectMapper.readValue(
            input.getBody(),
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
    } else if(input.toString().contains("X-Slack-Retry-Reason=http_timeout")) {
      // timeout measures
      return new GatewayResponse("Duplicate Slack Request (slack bot retry request when receiving request within 3000 milliseconds)", headers, 200);
    }

    var inputStr = input.getBody().replace("}\"", "}");
    Map<String, String> parsedMap = parseSlackRequest(inputStr);

    try {
      AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
          .withCredentials(new EnvironmentVariableCredentialsProvider())
          .withRegion(Regions.AP_NORTHEAST_1).build();
      InvokeRequest invokeRequest = new InvokeRequest()
          .withFunctionName(getFunctionName(awsLambda))
          .withPayload(getPayload(parsedMap.get("eventText")));
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

  private Map<String, String> parseSlackRequest(String inputStr) {
    Map<String, String> map = new HashMap<>();
    try {
      var allToMap = objectMapper.readValue(
          inputStr,
          new TypeReference<HashMap<String, Object>>() {
          }
      );
      var parsedEvent = objectMapper.readValue(
          objectMapper.writeValueAsString(allToMap.get("event")),
          new TypeReference<HashMap<String,Object>>() {}
      );
      map.put("eventText", parsedEvent.get("text").toString());
      map.put("userId", parsedEvent.get("user").toString());
    } catch (JsonProcessingException e) {
      System.err.println(e);
      System.exit(-1);
    }
    return map;
  }

  private String getPayload(String eventText) {
    int n = 1;
    boolean amountOnly;
    String period;

    // 出費を表す単語が含まれていたら出費のみ
    var amountPattern = Pattern.compile("(支出|出費|費用|消費|出勤)");
    amountOnly = amountPattern.matcher(eventText).find();

    // `\d + 日` ならn日分の費用に
    var periodPattern = Pattern.compile("\\d{1,3}日(分|前まで|間)");
    period = periodPattern.matcher(eventText).find() ? "day" : "month";

    // 何 月|日 分の情報を見るかを推定
    var nPattern = Pattern.compile("\\d{1,3}(ヶ月|日|か月)");
    Matcher matcher = nPattern.matcher(eventText);
    if (matcher.find()) {
      var nRep = eventText.replaceAll("(ヶ月|日|か月).+", "")
                          .replaceAll("<.+>\\s", "");
      if (nRep.matches("\\d{1,3}")) {
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
      client.send(request, BodyHandlers.ofString());
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
