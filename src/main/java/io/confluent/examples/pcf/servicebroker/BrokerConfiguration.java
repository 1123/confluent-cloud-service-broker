package io.confluent.examples.pcf.servicebroker;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BrokerConfiguration {

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