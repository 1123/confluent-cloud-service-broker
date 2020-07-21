package io.confluent.examples.pcf.servicebroker;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ServiceAccountAndApiKey {

    private String apiKey;
    private String apiSecret;
    private String serviceAccount;

}
