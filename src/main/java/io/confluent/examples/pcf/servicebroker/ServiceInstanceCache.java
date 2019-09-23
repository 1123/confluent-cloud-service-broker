package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class ServiceInstanceCache implements CommandLineRunner {

    @Value("${service.instance.store.topic}")
    private String serviceInstanceStoreTopic;

    @Autowired
    private KafkaConsumer<String,String> kafkaConsumer;

    @Autowired
    private AdminClient adminClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private Map<UUID, TopicServiceInstance> instances = new HashMap<>();

    @Autowired
    private TaskExecutor taskExecutor;

    private void createTopic() {
        CreateTopicsResult createTopicsResult = adminClient.createTopics(
                Collections.singleton(new NewTopic(serviceInstanceStoreTopic, 1, (short) 1))
        );
        try {
            createTopicsResult.all().get();
        } catch (ExecutionException | InterruptedException e) {
            log.info("Topic may already exist. ");
            log.info("e");
        }
    }

    @Override
    public void run(String... args) {
        createTopic();
        kafkaConsumer.subscribe(Collections.singletonList(serviceInstanceStoreTopic));
        taskExecutor.execute(() -> {
            while (true) {
                log.info("Caching service instances.");
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));
                log.info("Handling {} records", records.count());
                records.forEach(this::handleRecord);
            }
        });
    }

    private void handleRecord(ConsumerRecord<String, String> record) {
        UUID uuid = UUID.fromString(record.key());
        if (record.value() == null) { // tombstone message
            instances.remove(uuid);
            return;
        }
        try { // no tombstone message
            instances.put(uuid, objectMapper.readValue(record.value(), TopicServiceInstance.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
