package lambdaForZaim;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.HashMap;

import com.amazonaws.services.lambda.runtime.Context;
import lambdaForZaim.model.GatewayRequest;
import lambdaForZaim.model.GatewayRequest.QueryStringParameters;
import lambdaForZaim.model.GatewayResponse;
import lambdaForZaim.model.Money;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<GatewayRequest, GatewayResponse> {

  private OauthManager manager = new OauthManager();
  private ZaimApiRequester apiRequester = null;

  public GatewayResponse handleRequest(GatewayRequest input, final Context context) {
    manager.oauthSign();
    apiRequester = new ZaimApiRequester(
        manager.getService(), manager.getOauth1AccessToken()
    );
    var headers = new HashMap<String, String>() {{
      put("Content-Type", "application/json");
      put("X-Custom-Header", "application/json");
    }};
    QueryStringParameters params = input.getQueryStringParameters();
    int n = params.getN();
    boolean amountOnly = params.isAmountOnly();
    String period = params.getPeriod();
    Money money;

    if (period.contains("month")) {
      money = amountOnly ?
          apiRequester.requestAmountNMonthsAgo(n) :
          apiRequester.requestIncomeAndAmountNMonthsAgo(n);
    } else if (period.contains("day")) {
      money = amountOnly ?
          apiRequester.requestAmountNDaysAgo(n) :
          apiRequester.requestIncomeAndAmountNDaysAgo(n);
    } else {
      var output = "{ \"message\": \"invalid `period` value. `period` must be 'month' or 'day'.\" }";
      return new GatewayResponse(output, headers, 403);
    }

    var valueStr = amountOnly ? "出費" : "収支";
    var output = String.format(
        "%d%s前までの%sは%d円です．",
        n,
        period,
        valueStr,
        money.getTotal()
    );
    var message = "{ \"message\": \"" + output + money.toStringForCategory() + "\n\"}";
    return new GatewayResponse(message, headers, 200);
  }

}
