package io.confluent.examples.pcf.servicebroker;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Slf4j
class ApplicationTest {

    @Value("${service.uuid}") String serviceUUID;
    @Value("${service.plan.standard.uuid}") String servicePlanUUID;

    private RestTemplate restTemplate = new RestTemplate();

    @Test
    public void contextLoads() throws InterruptedException {
        Thread.sleep(1000);
    }

    @Test
    void testApi() {
        String serviceInstanceId = UUID.randomUUID().toString();
        createInstance(serviceInstanceId);
        createBinding(serviceInstanceId);
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
                "http://localhost:8080/v2/service_instances/" + serviceInstanceId,
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

    private void createBinding(String serviceInstanceId) {
        Map<String, Object> params = new HashMap<>();
        params.put("user", "jack");
        CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest =
                CreateServiceInstanceBindingRequest.builder()
                        .serviceInstanceId(serviceInstanceId)
                        .serviceDefinitionId(serviceUUID)
                        .planId(servicePlanUUID)
                        .bindResource(BindResource.builder().appGuid(UUID.randomUUID().toString()).build())
                        .parameters(params)
                        .build();
        String bindingId = UUID.randomUUID().toString();
        ResponseEntity<String> bindResult = restTemplate.exchange(
                "http://localhost:8080/v2/service_instances/" + serviceInstanceId + "/service_bindings/" + bindingId,
                HttpMethod.PUT,
                new HttpEntity<>(createServiceInstanceBindingRequest, headers()),
                String.class
        );
        log.info(bindResult.toString());
    }

}

