package io.confluent.examples.pcf.servicebroker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@SpringBootTest
class TopicCreatorTest {

    @Autowired private TopicCreator topicCreator;

    @Test
    public void test() throws ExecutionException, InterruptedException {
        topicCreator.create(UUID.randomUUID().toString(), 12, (short) 3);
    }

}