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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
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
import zaimtoslack.model.GatewayRequest;
import zaimtoslack.model.GatewayResponse;
import zaimtoslack.model.SlackRequest;
import zaimtoslack.model.ZaimResponse;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<GatewayRequest, Object> {
  private static ObjectMapper objectMapper = new ObjectMapper();
  private final String SLACK_BOT_USER_ACCESS_TOKEN = System.getenv("SLACK_BOT_USER_ACCESS_TOKEN");
  private final String SLACK_APP_AUTH_TOKEN = System.getenv("SLACK_APP_AUTH_TOKEN");
  private final String SLACK_USER_ID = System.getenv("SLACK_USER_ID");
  private final String SLACK_USER_NAME = System.getenv("SLACK_USER_NAME");
  private final String SLACK_CHANNEL_ID = System.getenv("SLACK_CHANNEL_ID");
  private final String SLACK_POST_URL = "https://slack.com/api/chat.postMessage";

  public Object handleRequest(final GatewayRequest input, final Context context) {
    Map<String, String> headers = new HashMap<>();
    String retText = "";
    String eventText = "";

    headers.put("Content-Type", "application/json");
    headers.put("X-Custom-Header", "application/json");

    // auth slack api
    if (input.toString().contains("\"challenge\"")) {
      try{
        Map<String, Object> map = objectMapper.readValue(input.getBody().toString(), new TypeReference<HashMap<String, String>>(){});
        if (map.containsKey("challenge")) {
          return new GatewayResponse("{\"challenge\": \"" + map.get("challenge") + "\"}", headers, 200);
        } else if (! map.containsKey("event")) {
          return new GatewayResponse("no contain `event` key", headers, 200);
        }
      } catch (IOException e) {
        System.err.println(e);
        System.exit(-1);
      }
    }

    SlackRequest slackRequest;
    try{
      slackRequest = objectMapper.readValue(input.getBody().toString(), SlackRequest.class);
      eventText = slackRequest.getEvent().getText();
      if (slackRequest.getEvent().getUser().equals(SLACK_USER_ID)) {
        System.out.println("speak by me");
        System.exit(0);
      }
    } catch (IOException e) {
      System.err.println(e);
    }

    slackPost(eventText);
    return new GatewayResponse(eventText, headers, 200);

    // try {
    //   AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
    //       .withCredentials(new EnvironmentVariableCredentialsProvider())
    //       .withRegion(Regions.AP_NORTHEAST_1).build();
    //   InvokeRequest invokeRequest = new InvokeRequest()
    //       .withFunctionName(getFunctionName(awsLambda))
    //       .withPayload("{\n" +
    //           "\"queryStringParameters\" : { \n"
    //           + "\"n\": 2,"
    //           + "\"amountOnly\": false,"
    //           + "\"period\": \"month\"}"
    //           + "}");
    //   InvokeResult invokeResult = awsLambda.invoke(invokeRequest);
    //   ZaimResponse zaimResponse = objectMapper.readValue(
    //       new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8),
    //       ZaimResponse.class
    //   );
    //   var totalString = zaimResponse.getTotalString();
    //   Map<String, Integer> amountForCategory = zaimResponse.getAmountForCategory();
    //   retText += totalString;
    //   for(Map.Entry<String, Integer> amount: amountForCategory.entrySet()) {
    //     retText += "\n" + amount.getKey() + ":  \t" + amount.getValue();
    //   }
    //   //write out the return value
    // } catch (IOException e) {
    //   System.err.println(e);
    // }
    // var body =  "{"
    //     + "\"token\": " + SLACK_APP_AUTH_TOKEN + ","
    //     + "\"channel\"" + SLACK_CHANNEL + ","
    //     + "\"text\"" + retText + ","
    //     + "\"username\"" + SLACK_USER_NAME + "}";
    // headers.put("Authorization", "Bearer" + SLACK_BOT_USER_ACCESS_TOKEN);
    // return new GatewayResponse(body, headers, 200);
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
      System.err.println(e);
      e.getStackTrace();
    }
    return "";
  }

  private void slackPost(String text) {
    try {
      HttpClient client = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder(URI.create(SLACK_POST_URL))
                                       .setHeader("Content-Type", "application/json")
                                       .setHeader("Authorization", "Bearer " + SLACK_BOT_USER_ACCESS_TOKEN)
                                       .POST(BodyPublishers.ofString(
                                           "{\"channel\":\"" + SLACK_CHANNEL_ID + "\","
                                               + "\"text\":\"" + text + "\","
                                               + "\"token\":\"" + SLACK_BOT_USER_ACCESS_TOKEN + "\"}"
                                       ))
                                       .build();
      HttpResponse response = client.send(request, BodyHandlers.ofString());
      System.out.println(response.statusCode());
      System.out.println(SLACK_BOT_USER_ACCESS_TOKEN);
      System.out.println(SLACK_CHANNEL_ID);
      System.out.println(response.body());
    } catch (IOException e) {
      System.err.println(e);
      System.exit(-1);
    } catch (InterruptedException e) {
      System.exit(-2);
    }
  }
}
