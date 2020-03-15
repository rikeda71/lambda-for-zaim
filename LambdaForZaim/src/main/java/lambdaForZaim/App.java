package lambdaForZaim;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import lambdaForZaim.model.GatewayRequest;
import lambdaForZaim.model.GatewayRequest.QueryStringParameters;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<GatewayRequest, lambdaForZaim.GatewayResponse> {

  private OauthManager manager = new OauthManager();
  private ZaimApiRequester apiRequester = null;

  public lambdaForZaim.GatewayResponse handleRequest(GatewayRequest input, final Context context) {
    manager.oauthSign();
    apiRequester = new ZaimApiRequester(
        manager.getService(), manager.getOauth1AccessToken()
    );
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("X-Custom-Header", "application/json");
    QueryStringParameters params = input.getQueryStringParameters();
    int n = params.getN();
    boolean amountOnly = params.isAmountOnly();
    String period = params.getPeriod();
    lambdaForZaim.Money money;

    if (period.contains("month")) {
      money = amountOnly ?
          apiRequester.requestAmountNMonthsAgo(n) :
          apiRequester.requestIncomeAndAmountNMonthsAgo(n);
    } else if (period.contains("day")) {
      money = amountOnly ?
          apiRequester.requestAmountNDaysAgo(n) :
          apiRequester.requestIncomeAndAmountNDaysAgo(n);
    } else {
      String output = "{ \"message\": \"invalid `period` value. `period` must be 'month' or 'day'.\" }";
      return new lambdaForZaim.GatewayResponse(output, headers, 403);
    }
    String valueStr = amountOnly ? "出費" : "収支";

    String output = String.format(
        "%d%s前までの%sは%d円です．",
        n,
        period,
        valueStr,
        money.getTotal()
    );
    for (Map.Entry<String, Integer> entry: money.getAmountForCategory().entrySet()) {
      output += "\n" + entry.getKey() + ":\t" + entry.getValue();
    }
    String message = "{ \"message\": \"" + output + "\"}";

    return new lambdaForZaim.GatewayResponse(message, headers, 200);
  }

}
