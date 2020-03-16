package zaimtoslack;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import zaimtoslack.model.GatewayResponse;
import zaimtoslack.model.SlackRequest;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<SlackRequest, Object> {
  private static ObjectMapper objectMapper = new ObjectMapper();

  public Object handleRequest(final SlackRequest input, final Context context) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("X-Custom-Header", "application/json");
    try {
      AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
          .withCredentials(new EnvironmentVariableCredentialsProvider())
          .withRegion(Regions.AP_NORTHEAST_1).build();
      InvokeRequest invokeRequest = new InvokeRequest()
          .withFunctionName("LambdaForZaim-LambdaForZaim-96CMS8XH70KT")
          .withPayload("{\n" +
              "\"queryStringParameters\" : { \n"
              + "\"n\": 2,"
              + "\"amountOnly\": false,"
              + "\"period\": \"month\"}"
              + "}");
      InvokeResult invokeResult = awsLambda.invoke(invokeRequest);
      Map<String, Map<String, String>> zaimResponse = objectMapper.readValue(
          new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8),
          new TypeReference<HashMap<String, Map<String, String>>>(){});
      String message = zaimResponse.get("body").get("message");
      //write out the return value
    } catch (IOException e) {
      System.err.println(e);
    }
    return new GatewayResponse("hello world", headers, 200);
  }
}
