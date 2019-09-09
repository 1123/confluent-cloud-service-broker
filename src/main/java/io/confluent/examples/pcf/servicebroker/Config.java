package io.confluent.examples.pcf.servicebroker;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;
import java.util.UUID;


@Configuration
public class Config {

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${service.instance.consumer.group.id}")
    private String serviceInstanceConsumerGroupId;

    @Bean
    public AdminClient adminClient() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("retry.backoff.ms", "500");
        properties.put("request.timeout.ms", "20000");
        return AdminClient.create(properties);
    }

    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        Properties producerProperties = new Properties();
        producerProperties.put("bootstrap.servers", bootstrapServers);
        producerProperties.put("key.serializer", StringSerializer.class);
        producerProperties.put("value.serializer", StringSerializer.class);
        return new KafkaProducer<>(producerProperties);
    }

    @Bean
    public KafkaConsumer<String, String> kafkaConsumer() {
        Properties consumerProperties = new Properties();
        consumerProperties.put("bootstrap.servers", bootstrapServers);
        consumerProperties.put("key.deserializer", StringDeserializer.class);
        consumerProperties.put("value.deserializer", StringDeserializer.class);
        consumerProperties.put("group.id", UUID.randomUUID().toString()); // always read from the beginning.
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(consumerProperties);
    }

    @Bean
    public TaskExecutor getTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(1);
        threadPoolTaskExecutor.setMaxPoolSize(1);
        return threadPoolTaskExecutor;
    }

    @Bean
    public Catalog catalog() {
        Plan plan = Plan.builder()
                .id("standard")
                .name("standard")
                .description("Provision a topic with 3 partitions and replication factor 3.")
                .free(true)
                .build();

        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .id("confluent-kafka")
                .name("Apache Kafka Service Broker for Confluent Cloud")
                .description("A service for provisioning Kafka Topics on Confluent Cloud")
                .bindable(true)
                .tags("example", "tags")
                .plans(plan)
                .build();

        return Catalog.builder()
                .serviceDefinitions(serviceDefinition)
                .build();
    }
}