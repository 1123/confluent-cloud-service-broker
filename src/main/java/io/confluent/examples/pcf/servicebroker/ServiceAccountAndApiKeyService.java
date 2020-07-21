package io.confluent.examples.pcf.servicebroker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Dummy implementation. This must be improved to either
// * use a future API (currently none exists, but there should be one available soon)
// * call the cli,
// * or pick a set of credentials from a preconfigured pool of credentials.
//   These credentials should, however, never be reused.

@Service
public class ServiceAccountAndApiKeyService {

    @Value( "${broker.clientcredentials.serviceAccount}" )
    private String serviceAccount;

    @Value( "${broker.clientcredentials.apiKey}" )
    private String apiKey;

    @Value( "${broker.clientcredentials.apiSecret}" )
    private String apiSecret;

    public ServiceAccountAndApiKey getCredentials() {
        return ServiceAccountAndApiKey.builder()
                .serviceAccount(serviceAccount)
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .build();
    }

}

