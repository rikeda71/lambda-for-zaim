package zaimtoslack.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class ZaimResponse {

  @Getter @Setter private String totalString;
  @Getter @Setter private Map<String, Integer> amountForCategory;
  @Getter @Setter private Map<String, String> headers;
  @Getter @Setter private int statusCode;

  public ZaimResponse(){}

  public ZaimResponse(final String totalString, final Map<String, Integer> amountForCategory, final Map<String, String> headers, final int statusCode) {
    this.totalString = totalString;
    this.amountForCategory = amountForCategory;
    this.statusCode = statusCode;
    this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
  }
}
