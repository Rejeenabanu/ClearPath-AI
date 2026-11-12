package com.clearpath.clearpath_api.service;

import com.clearpath.clearpath_api.config.PayPalProperties;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;
import org.springframework.stereotype.Service;


@Service
public class PayPalAuthService {

private final PayPalProperties properties;
private final RestClient restClient;

public PayPalAuthService(PayPalProperties properties) {
    this.properties = properties;
    this.restClient = RestClient.builder()
            .baseUrl(properties.baseUrl())
            .build();
}

public String getAccessToken() {
    var form = new LinkedMultiValueMap<String, String>();
    form.add("grant_type", "client_credentials");

    Map<?, ?> response = restClient.post()
            .uri("/v1/oauth2/token")
            .headers(headers -> headers.setBasicAuth(
                    properties.clientId(),
                    properties.clientSecret()))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

    if (response == null || response.get("access_token") == null) {
        throw new IllegalStateException(
                "PayPal did not return an access token");
    }

    return response.get("access_token").toString();
}
}

