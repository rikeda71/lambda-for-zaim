package zaimtoslack.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class SlackRequest {
  @Getter @Setter private String token;
  @Getter @Setter private String team_id;
  @Getter @Setter private String api_app_id;
  @Getter @Setter private Event event;
  @Getter @Setter private String type;
  @Getter @Setter private String event_id;
  @Getter @Setter private Long event_time;
  @Getter @Setter private List<String> authed_users;

  public SlackRequest() {}

  public static class Event {
    @Getter @Setter private String type;
    @Getter @Setter private String user;
    @Getter @Setter private String text;
    @Getter @Setter private String ts;
    @Getter @Setter private String channel;
    @Getter @Setter private String event_ts;
  }
}
