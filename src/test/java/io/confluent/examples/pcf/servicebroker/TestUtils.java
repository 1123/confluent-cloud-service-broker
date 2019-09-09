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

    public static void recreateServiceInstancesTopic() throws ExecutionException, InterruptedException {
        DeleteTopicsResult deleteTopicsResult = adminClient().deleteTopics(Collections.singleton("kafka.service.broker.service-instances"));
        deleteTopicsResult.all().get();
        CreateTopicsResult createTopicsResult = adminClient().createTopics(Collections.singleton(new NewTopic("kafka.service.broker.service-instances", 1, (short) 1)));
        createTopicsResult.all().get();
    }

    private static AdminClient adminClient() {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        return AdminClient.create(properties);
    }

    @Test
    @Ignore
    void deleteServiceInstancesTopic() throws InterruptedException, ExecutionException {
        DeleteTopicsResult result = adminClient().deleteTopics(Collections.singleton("kafka.service.broker.service-instances"));
        result.all().get();
    }

    @Test
    @Ignore
    void listTopics() throws InterruptedException, ExecutionException {
        ListTopicsResult result = adminClient().listTopics();
        log.info(String.valueOf(result.names().get()));
    }





}