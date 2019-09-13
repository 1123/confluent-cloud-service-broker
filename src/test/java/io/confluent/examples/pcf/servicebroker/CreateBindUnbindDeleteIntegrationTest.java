package io.confluent.examples.pcf.servicebroker;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class CreateBindUnbindDeleteIntegrationTest {

    @Value("${service.uuid}") String serviceUUID;
    @Value("${service.plan.standard.uuid}") String servicePlanUUID;

    private RestTemplate restTemplate = new RestTemplate();

    @LocalServerPort
    private int port;

    private String url() {
        return "http://localhost:" + port + "/v2/service_instances/";
    }

    @Test
    void testApi() {
        String serviceInstanceId = UUID.randomUUID().toString();
        createInstance(serviceInstanceId);
        String bindingId = UUID.randomUUID().toString();
        createBinding(serviceInstanceId, bindingId);
        removeBinding(serviceInstanceId, bindingId);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("X-Broker-API-Version", Collections.singletonList("2.12"));
        return headers;
    }

    private void createInstance(String serviceInstanceId) {
        Map<String, Object> params = new HashMap<>();
        params.put("topic_name", UUID.randomUUID().toString());
        ResponseEntity<String> createResult = restTemplate.exchange(
                url() + serviceInstanceId,
                HttpMethod.PUT,
                new HttpEntity<>(
                        CreateServiceInstanceRequest.builder()
                                .planId(servicePlanUUID)
                                .serviceDefinitionId(serviceUUID)
                                .parameters(params)
                                .build(),
                        headers()
                ),
                String.class
        );
        log.info(createResult.toString());
    }

    private void createBinding(String serviceInstanceId, String bindingId) {
        Map<String, Object> params = new HashMap<>();
        params.put("user", "User:admin");
        CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest =
                CreateServiceInstanceBindingRequest.builder()
                        .serviceInstanceId(serviceInstanceId)
                        .serviceDefinitionId(serviceUUID)
                        .planId(servicePlanUUID)
                        .bindResource(
                                BindResource.builder()
                                        .appGuid(UUID.randomUUID().toString())
                                        .build()
                        )
                        .parameters(params)
                        .build();
        ResponseEntity<String> bindResult = restTemplate.exchange(
                url() + serviceInstanceId + "/service_bindings/" + bindingId
                        + "?",
                HttpMethod.PUT,
                new HttpEntity<>(createServiceInstanceBindingRequest, headers()),
                String.class
        );
        log.info(bindResult.toString());
    }

    private void removeBinding(String serviceInstanceId, String bindingId) {
        restTemplate.delete(
                url() + serviceInstanceId + "/service_bindings/" + bindingId
                        + "?service_id=" + serviceUUID + "&plan_id=" + servicePlanUUID
        );
    }

}

