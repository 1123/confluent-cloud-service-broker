package io.confluent.examples.pcf.servicebroker;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.Properties;
import java.util.UUID;

@Configuration
public class Config {

    @Bean
    public AdminClient adminClient(
            @Value("${broker.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${broker.kafka.sasl-jaas-config}") String saslJaasConfig,
            @Value("${broker.kafka.security-protocol}") String securityProtocol
    ) {
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
    public KafkaProducer<String, String> kafkaProducer(
            @Value("${broker.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${broker.kafka.sasl-jaas-config}") String saslJaasConfig,
            @Value("${broker.kafka.security-protocol}") String securityProtocol
    ) {
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
    public KafkaConsumer<String, String> kafkaConsumer(
            @Value("${broker.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${broker.kafka.sasl-jaas-config}") String saslJaasConfig,
            @Value("${broker.kafka.security-protocol}") String securityProtocol
    ) {
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

}