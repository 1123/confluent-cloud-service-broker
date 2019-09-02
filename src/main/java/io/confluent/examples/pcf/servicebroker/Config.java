package io.confluent.examples.pcf.servicebroker;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;


@Configuration
public class Config {

    @Value("${kafka.sasl.jaas.config}")
    private String saslJaasConfig;
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    @Bean
    public AdminClient adminClient() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("ssl.endpoint.identification.algorithm", "https");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("retry.backoff.ms", "500");
        properties.put("request.timeout.ms", "20000");
        properties.put("sasl.jaas.config", saslJaasConfig);
        properties.put("security.protocol", "SASL_SSL");
        return AdminClient.create(properties);
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