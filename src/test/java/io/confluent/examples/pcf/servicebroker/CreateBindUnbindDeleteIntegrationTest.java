package io.confluent.examples.pcf.servicebroker;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@ExtendWith(KafkaJunitExtension.class)
public class CreateBindUnbindDeleteIntegrationTest {

    private String serviceUUID = null;
    private String servicePlanUUID = null;
    private int port = 0;
    private RestTemplate restTemplate = null;
    private String topicName;
    private String saslJaasConfig;

    @Autowired
    private ConfigurableApplicationContext configurableApplicationContext;

    public CreateBindUnbindDeleteIntegrationTest(
            @Value("${service.uuid}") String serviceUUID,
            @Value("${service.plan.standard.uuid}") String servicePlanUUID,
            @LocalServerPort int port,
            @Value("${sasl.jaas.config}") String saslJaasConfig,
            ConfigurableApplicationContext configurableApplicationContext) {
        this.serviceUUID = serviceUUID;
        this.servicePlanUUID = servicePlanUUID;
        this.port = port;
        this.topicName = UUID.randomUUID().toString();
        this.restTemplate = new RestTemplate();
        this.saslJaasConfig = saslJaasConfig;
    }

    private String url() {
        return "http://localhost:" + port + "/v2/service_instances/";
    }

    @Test
    void testApi() throws ExecutionException, InterruptedException {
        String serviceInstanceId = UUID.randomUUID().toString();
        createInstance(serviceInstanceId);
        // TODO: we should better wait in the createServiceInstanceBinding method on the server for 5 seconds time.
        Thread.sleep(5000);
        String bindingId = UUID.randomUUID().toString();
        createBinding(serviceInstanceId, bindingId);
        testProducing();
        testConsuming();
        removeBinding(serviceInstanceId, bindingId);
        // TODO: deleteService
        // It is important to close the producer, consumer and the admin client
        // prior to zookeeper and kafka being shut down.
        // Otherwise the test will hang for quite some time.
        log.info("Closing application context");
        configurableApplicationContext.close();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.put("X-Broker-API-Version", Collections.singletonList("2.12"));
        return headers;
    }

    private void createInstance(String serviceInstanceId) {
        Map<String, Object> params = new HashMap<>();
        params.put("topic_name", topicName);
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
        params.put("user", "User:client");
        params.put("consumerGroup", "sampleConsumerGroup");
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

    private void testProducing() throws ExecutionException, InterruptedException {
        KafkaProducer<String, String> sampleProducer = sampleProducer();
        Future<RecordMetadata> result = sampleProducer.send(new ProducerRecord<>(topicName, "key1", "value1"));
        RecordMetadata recordMetadata = result.get();
        log.info(recordMetadata.toString());
        sampleProducer.close();
    }

    private void testConsuming() {
        KafkaConsumer<String, String> sampleConsumer = sampleConsumer();
        sampleConsumer.subscribe(Collections.singleton(topicName));
        ConsumerRecords<String, String> result = sampleConsumer.poll(Duration.ofSeconds(10));
        assertEquals(1, result.records(new TopicPartition(topicName, 0)).size());
        log.info("Received result: " + result.toString());
        sampleConsumer.close();
    }

    private void removeBinding(String serviceInstanceId, String bindingId) {
        restTemplate.delete(
                url() + serviceInstanceId + "/service_bindings/" + bindingId
                        + "?service_id=" + serviceUUID + "&plan_id=" + servicePlanUUID
        );
    }

    private KafkaConsumer<String, String> sampleConsumer() {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:10091");
        properties.put("retry.backoff.ms", "500");
        properties.put("request.timeout.ms", "20000");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("security.protocol", "SASL_PLAINTEXT");
        properties.put("key.deserializer", StringDeserializer.class);
        properties.put("value.deserializer", StringDeserializer.class);
        properties.put("group.id", "sampleConsumerGroup");
        properties.put("auto.offset.reset", "earliest");
        properties.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"client\" password=\"client-secret\";");
        return new KafkaConsumer<>(properties);
    }

    private KafkaProducer<String, String> sampleProducer() {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:10091");
        properties.put("retry.backoff.ms", "500");
        properties.put("request.timeout.ms", "20000");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("security.protocol", "SASL_PLAINTEXT");
        properties.put("key.serializer", StringSerializer.class);
        properties.put("value.serializer", StringSerializer.class);
        properties.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"client\" password=\"client-secret\";");
        return new KafkaProducer<>(properties);
    }

}

