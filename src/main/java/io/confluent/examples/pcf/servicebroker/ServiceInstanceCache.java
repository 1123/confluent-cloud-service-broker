package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.ConfigResource;
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

    @Value("${broker.store.topic.name}")
    private String serviceInstanceStoreTopic;

    @Value("${broker.store.topic.replication}")
    private short replicationFactor;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private Map<UUID, TopicServiceInstance> instances = new HashMap<>();

    private TaskExecutor taskExecutor;
    private AdminClient adminClient;
    private KafkaConsumer<String,String> kafkaConsumer;

    public ServiceInstanceCache(
            KafkaConsumer<String, String> kafkaConsumer,
            TaskExecutor taskExecutor,
            AdminClient adminClient
    ) {
        this.kafkaConsumer = kafkaConsumer;
        this.taskExecutor = taskExecutor;
        this.adminClient = adminClient;
    }

    private void createServiceInstancesTopic() throws InterruptedException {
        log.info("Creating service instances topic");
        CreateTopicsResult createTopicsResult = adminClient.createTopics(
                Collections.singleton(new NewTopic(serviceInstanceStoreTopic, 1, replicationFactor))
        );
        try {
            createTopicsResult.all().get();
        } catch (ExecutionException e) {
            log.info("Topic may already exist. ");
            log.info(e.getMessage());
        }
    }

    private void ensureServiceInstancesTopicIsLogCompacted() throws ExecutionException, InterruptedException {
        log.info("Setting service instances topic to cleanup.policy log compacted ");
        Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>();
        configs.put(
                new ConfigResource(ConfigResource.Type.TOPIC, serviceInstanceStoreTopic),
                Collections.singleton(
                        new AlterConfigOp(
                                new ConfigEntry("cleanup.policy", "compact"),
                                AlterConfigOp.OpType.SET
                        )
                )
        );
        AlterConfigsResult alterConfigsResult = adminClient.incrementalAlterConfigs(configs);
        log.info("AlterConfigsResult: " + alterConfigsResult.all().get());
    }

    @Override
    public void run(String... args) throws ExecutionException, InterruptedException {
        createServiceInstancesTopic();
        ensureServiceInstancesTopicIsLogCompacted();
        kafkaConsumer.subscribe(Collections.singletonList(serviceInstanceStoreTopic));
        taskExecutor.execute(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                log.info("Caching service instances.");
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(Long.MAX_VALUE));
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
