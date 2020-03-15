package lambdaForZaim.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * POJO containing response object for API Gateway.
 */
public class GatewayResponse {

    @Getter private final String body;
    @Getter private final Map<String, String> headers;
    @Getter private final int statusCode;

    public GatewayResponse(final String body, final Map<String, String> headers, final int statusCode) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
    }

}
