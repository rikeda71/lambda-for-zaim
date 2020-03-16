package zaimtoslack.model;

import lombok.Getter;
import lombok.Setter;

public class SlackRequest {
  @Getter @Setter private String token;
  @Getter @Setter private String challenge;
  @Getter @Setter private String type;

  public SlackRequest() {}
  public SlackRequest(String token, String challenge, String type) {
    this.token = token;
    this.challenge = challenge;
    this.type = type;
  }
}
