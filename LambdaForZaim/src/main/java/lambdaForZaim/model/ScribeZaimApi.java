package lambdaForZaim.model;

import com.github.scribejava.core.builder.api.DefaultApi10a;

public class ScribeZaimApi extends DefaultApi10a {

    private static final String AUTHORIZE_URL = "https://auth.zaim.net/users/auth";
    private static final String PROVIDER_URL = "https://api.zaim.net/v2/auth/";
    private static final String REQUEST_TOKEN_RESOURCE = PROVIDER_URL + "request";
    private static final String ACCESS_TOKEN_RESOURCE = PROVIDER_URL + "access";
    protected ScribeZaimApi() {

    }

    private static class InstanceHolder {
        private static final ScribeZaimApi INSTANCE = new ScribeZaimApi();
    }

    public static ScribeZaimApi instance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_RESOURCE;
    }

    @Override
    public String getRequestTokenEndpoint() {
        return REQUEST_TOKEN_RESOURCE;
    }

    @Override
    public String getAuthorizationBaseUrl() {
        return AUTHORIZE_URL;
    }

}
