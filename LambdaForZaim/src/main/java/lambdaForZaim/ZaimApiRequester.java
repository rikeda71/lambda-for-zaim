package lambdaForZaim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import lambdaForZaim.model.Money;
import lombok.Data;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ZaimApiRequester {

  private OAuth10aService service;
  private OAuth1AccessToken accessToken;
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("y-M-d");
  private static ObjectMapper objectMapper = new ObjectMapper();

  // zaim end points
  private static final String moneyURL = "https://api.zaim.net/v2/home/money";
  private static final String categoryURL = "https://api.zaim.net/v2/home/category";

  private Map<Integer, String> idToCategories;

  @Data
  private static class Category {
    public int id;
    public String mode;
    public String name;
    public int sort;
    public int active;
    public String modified;
    public int parent_category_id;
    public int local_id;
  }

  @Data
  private static class Categories {
    private List<Category> categories;
  }

  ZaimApiRequester(OAuth10aService service, OAuth1AccessToken accessToken) {
    this.service = service;
    this.accessToken = accessToken;
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    requestMyCategories();
  }

  public void requestMyCategories() {
    idToCategories = new HashMap<>();
    OAuthRequest request = new OAuthRequest(Verb.GET, categoryURL);
    service.signRequest(accessToken, request);
    try {
      Response res = service.execute(request);
      String jsonStr = res.getBody();
      Categories jsonmap = objectMapper.readValue(jsonStr, Categories.class);
      for (Category tmp: jsonmap.categories) {
        idToCategories.put((Integer)tmp.id, (String)tmp.name);
      }
    } catch (ExecutionException e) {
      System.err.println(e);
      System.exit(-1);
    } catch (IOException e) {
      System.err.println(e);
      System.exit(-2);
    } catch (InterruptedException e) {
      System.err.println(e);
      System.exit(-3);
    }
  }

  /**
   * request zaim API to income and amount spent n days ago
   * @param n : show how many months
   * @return Income and amount spent n months ago
   */
  public Money requestIncomeAndAmountNDaysAgo(int n) {
    return requestMoneyWithDates(
        LocalDate.now().minusDays(n).format(formatter),
        LocalDate.now().format(formatter),
        true
    );
  }

  /**
   * request zaim API to income and amount spent n months ago
   * @param n : show how many months
   * @return Income and amount spent n months ago
   */
  public Money requestIncomeAndAmountNMonthsAgo(int n) {
    return requestMoneyWithDates(
        LocalDate.now().minusMonths(n).format(formatter),
        LocalDate.now().format(formatter),
        true
    );
  }

  /**
   * request zaim API to amount spent n days ago
   * @param n : show how many months
   * @return Total amount spent n months ago
   */
  public Money requestAmountNDaysAgo(int n) {
    return requestMoneyWithDates(
        LocalDate.now().minusDays(n).format(formatter),
        LocalDate.now().format(formatter),
        false
    );
  }

  /**
   * request zaim API to amount spent n months ago
   * @param n : show how many months
   * @return Total amount spent n months ago
   */
  public Money requestAmountNMonthsAgo(int n) {
    return requestMoneyWithDates(
        LocalDate.now().minusMonths(n).format(formatter),
        LocalDate.now().format(formatter),
        false
    );
  }

  /**
   *
   * @param start the date string of `from` ex.) '2019-10-9'
   * @param end the date string of `to`
   * @return Total money spent in the period given by the arguments
   */
  private Money requestMoneyWithDates(String start, String end, Boolean usedOnly) {
    int totalMoney = 0;
    Map<String, Integer> totals = new HashMap<>();
    OAuthRequest request = new OAuthRequest(Verb.GET, moneyURL);

    Map<String, String> map = new HashMap<>();
    map.put("start_date", start);
    map.put("end_date", end);
    map.forEach((k, v) -> request.addParameter(k, v));
    service.signRequest(accessToken, request);

    try {
      Response res = service.execute(request);
      String jsonStr = res.getBody();
      Map<String, Object> jsonmap = objectMapper.readValue(jsonStr, new TypeReference<HashMap<String, Object>>(){});
      for (Map<String, Object> moneyObj: ((List<Map<String, Object>>)jsonmap.get("money"))) {
        String category = idToCategories.get((Integer) moneyObj.get("category_id"));
        if ("payment".equals(moneyObj.get("mode"))) {
          if (totals.containsKey(category)) {
            totals.put(category, totals.get(category) - (int)moneyObj.get("amount"));
          } else {
            totals.put(category, -(int)moneyObj.get("amount"));
          }
          totalMoney += (int)moneyObj.get("amount");
        } else if (usedOnly && "income".equals(moneyObj.get("mode"))) {
          if (totals.containsKey("収入")) {
            totals.put("収入", totals.get("収入") + (int)moneyObj.get("amount"));
          } else {
            totals.put("収入", (int)moneyObj.get("amount"));
          }
          totalMoney -= (int)moneyObj.get("amount");
        }
      }
    } catch (ExecutionException e) {
      System.err.println(e);
      System.exit(-1);
    } catch (IOException e) {
      System.err.println(e);
      System.exit(-2);
    } catch (InterruptedException e) {
      System.err.println(e);
      System.exit(-3);
    }

    Money money = new Money(totalMoney, totals);
    return money;
  }
}
