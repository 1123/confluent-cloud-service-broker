package io.confluent.examples.pcf.servicebroker;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Slf4j
class TestUtils {

    static void recreateServiceInstancesTopic() throws ExecutionException, InterruptedException {
        DeleteTopicsResult deleteTopicsResult = adminClient().deleteTopics(Collections.singleton("kafka.service.broker.service-instances"));
        deleteTopicsResult.all().get();
        CreateTopicsResult createTopicsResult = adminClient().createTopics(Collections.singleton(new NewTopic("kafka.service.broker.service-instances", 1, (short) 1)));
        createTopicsResult.all().get();
    }

    private static AdminClient adminClient() {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9093");
        properties.put("retry.backoff.ms", "500");
        properties.put("request.timeout.ms", "20000");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("security.protocol", "SASL_PLAINTEXT");
        properties.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin-secret\";");
        return AdminClient.create(properties);
    }

    @Test
    @Ignore
    void deleteServiceInstancesTopic() throws InterruptedException, ExecutionException {
        DeleteTopicsResult result = adminClient().deleteTopics(Collections.singleton("kafka.service.broker.service-instances"));
        result.all().get();
    }

    @Test
    void listTopics() throws InterruptedException, ExecutionException {
        ListTopicsResult result = adminClient().listTopics();
        log.info(String.valueOf(result.names().get()));
    }

}