package io.confluent.examples.pcf.servicebroker;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@Service
public class TopicCreator {

    @Autowired
    AdminClient adminClient;

    public void create(String topicName, int partitions, short replicationFactor) throws ExecutionException, InterruptedException {
            CreateTopicsResult result = adminClient.createTopics(Arrays.asList(new NewTopic(topicName, partitions, replicationFactor)));
        result.all().get();
    }

}
