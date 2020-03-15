package lambdaForZaim;

import java.util.Map;
import lombok.Data;

@Data
public class Money {
  private int total;
  private Map<String, Integer> amountForCategory;

  Money() {

  }

  Money(int total, Map<String, Integer> amountForCategory) {
    this.total = total;
    this.amountForCategory = amountForCategory;
  }
}
