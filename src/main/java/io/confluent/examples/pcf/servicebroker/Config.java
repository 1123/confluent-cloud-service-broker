package io.confluent.examples.pcf.servicebroker;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.model.catalog.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Configuration
public class Config {

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${sasl.jaas.config}")
    private String saslJaasConfig;

    @Value("${security.protocol}")
    private String securityProtocol;

    @Bean
    public AdminClient adminClient() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("retry.backoff.ms", "500");
        properties.put("request.timeout.ms", "20000");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("security.protocol", securityProtocol);
        properties.put("sasl.jaas.config", saslJaasConfig);
        return AdminClient.create(properties);
    }

    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("client.id", "kafka-service-broker-client");
        properties.put("key.serializer", StringSerializer.class);
        properties.put("value.serializer", StringSerializer.class);
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("security.protocol", securityProtocol);
        properties.put("sasl.jaas.config", saslJaasConfig);
        return new KafkaProducer<>(properties);
    }

    @Bean
    public KafkaConsumer<String, String> kafkaConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("key.deserializer", StringDeserializer.class);
        properties.put("value.deserializer", StringDeserializer.class);
        properties.put("group.id", UUID.randomUUID().toString()); // always read from the beginning.
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("client.id", "kafka-service-broker-client");
        properties.put("security.protocol", securityProtocol);
        properties.put("sasl.jaas.config", saslJaasConfig);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(properties);
    }

    @Bean
    public TaskExecutor getTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(1);
        threadPoolTaskExecutor.setMaxPoolSize(1);
        return threadPoolTaskExecutor;
    }

    private Map<String, Object> createParameters() {
        Map<String, Object> createProperties = new HashMap<>();
        Map<String, Object> topicNameProperties = new HashMap<>();
        topicNameProperties.put("description", "name des topics");
        topicNameProperties.put("type", "string");
        createProperties.put("topicNames", topicNameProperties);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("[$schema]", "http://json-schema.org/draft-04/schema#");
        parameters.put("description", "Name des Topics");
        parameters.put("type", "object");
        parameters.put("properties", createProperties);
        return parameters;
    }

    private Schemas schemas() {
        return Schemas.builder()
                .serviceInstanceSchema(
                        ServiceInstanceSchema.builder()
                                .createMethodSchema(
                                        MethodSchema.builder()
                                                .parameters(createParameters())
                                                .build()
                                )
                                .build()
                )
                .serviceBindingSchema(
                        ServiceBindingSchema.builder()
                                .createMethodSchema(
                                        MethodSchema.builder()
                                                .parameters(createParameters())
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    @Bean
    public Catalog catalog(
            @Value("${service.uuid}") String serviceUUID,
            @Value("${service.name}") String serviceName,
            @Value("${service.plan.standard.uuid}") String servicePlanUUID,
            @Value("${service.plan.standard.name}") String standardServicePlan
    ) {
        Plan plan = Plan.builder()
                .id(servicePlanUUID)
                .name(standardServicePlan)
                .description("Provision a topic with 3 partitions and replication factor 3.")
                //.schemas(schemas())
                .free(true)
                .build();

        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .id(serviceUUID)
                .name(serviceName)
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