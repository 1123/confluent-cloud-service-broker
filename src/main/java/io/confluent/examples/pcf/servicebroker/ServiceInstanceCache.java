package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Component
@Slf4j
public class ServiceInstanceCache implements CommandLineRunner {

    @Value("${service.instance.store.topic}")
    private String serviceInstanceStoreTopic;

    @Autowired
    private KafkaConsumer<String,String> kafkaConsumer;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private Map<UUID, TopicServiceInstance> instances = new HashMap<>();

    @Autowired
    private TaskExecutor taskExecutor;

    @Override
    public void run(String... args) throws Exception {
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
