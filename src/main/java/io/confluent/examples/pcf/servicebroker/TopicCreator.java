package io.confluent.examples.pcf.servicebroker;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
class TopicCreator {

    @Autowired
    AdminClient adminClient;

    void create(String topicName, int partitions, short replicationFactor) throws ExecutionException, InterruptedException {
        CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(new NewTopic(topicName, partitions, replicationFactor)));
        log.info("Creating topic");
        result.all().get();
    }

}
