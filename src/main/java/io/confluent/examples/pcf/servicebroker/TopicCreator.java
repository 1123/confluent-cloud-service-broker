package io.confluent.examples.pcf.servicebroker;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Service
public class TopicCreator {

    @Value("${kafka.sasl.jaas.config}")
    private String saslJaasConfig;
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    public void create(String topicName, int partitions, short replicationFactor) throws ExecutionException, InterruptedException {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("ssl.endpoint.identification.algorithm", "https");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("retry.backoff.ms", "500");
        properties.put("request.timeout.ms", "20000");
        properties.put("sasl.jaas.config", saslJaasConfig);
        properties.put("security.protocol", "SASL_SSL");
        AdminClient adminClient = AdminClient.create(properties);
        CreateTopicsResult result = adminClient.createTopics(Arrays.asList(new NewTopic(topicName, partitions, replicationFactor)));
        result.all().get();
    }

}
