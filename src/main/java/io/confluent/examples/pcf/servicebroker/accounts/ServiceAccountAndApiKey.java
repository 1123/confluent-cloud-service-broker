package io.confluent.examples.pcf.servicebroker.accounts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAccountAndApiKey {

    private String apiKey;
    private String apiSecret;
    private String serviceAccount;

}
