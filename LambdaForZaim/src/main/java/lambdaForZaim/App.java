package lambdaForZaim;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import lombok.Getter;
import lombok.Setter;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<App.Request, Object> {

  private OauthManager manager = new OauthManager();
  private ZaimApiRequester apiRequester = null;

  public static class Request {
    @Getter @Setter int n;
    @Getter @Setter boolean amountOnly;
    @Getter @Setter String period;

    public Request() {
    }

    public Request(int n, boolean amountOnly, String period) {
      this.n = n;
      this.amountOnly = amountOnly;
      this.period = period;
    }

  }

  public GatewayResponse handleRequest(final Request input, final Context context) {
    manager.oauthSign();
    apiRequester = new ZaimApiRequester(
        manager.getService(), manager.getOauth1AccessToken()
    );
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("X-Custom-Header", "application/json");
    Money money;
    if (input.period.contains("month")) {
      money = input.amountOnly ?
          apiRequester.requestAmountNMonthsAgo(input.n) :
          apiRequester.requestIncomeAndAmountNMonthsAgo(input.n);
    } else if (input.period.contains("day")) {
      money = input.amountOnly ?
          apiRequester.requestAmountNDaysAgo(input.n) :
          apiRequester.requestIncomeAndAmountNDaysAgo(input.n);
    } else {
      String output = "{ \"message\": \"invalid `period` value. `period` must be 'month' or 'day'.\" }";
      return new GatewayResponse(output, headers, 403);
    }
    String valueStr = input.amountOnly ? "出費" : "収支";

    var output = String.format(
        "%d%s前までの%sは%d円です．",
        input.n,
        input.period,
        valueStr,
        money.getTotal()
    );
    for (Map.Entry<String, Integer> entry: money.getAmountForCategory().entrySet()) {
      output += "\n" + entry.getKey() + ":\t" + entry.getValue();
    }
    var message = "{ \"message\": \"" + output + "\"}";

    return new GatewayResponse(message, headers, 200);
  }

  private String getPageContents(String address) throws IOException {
    URL url = new URL(address);
    try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
      return br.lines().collect(Collectors.joining(System.lineSeparator()));
    }
  }
}
