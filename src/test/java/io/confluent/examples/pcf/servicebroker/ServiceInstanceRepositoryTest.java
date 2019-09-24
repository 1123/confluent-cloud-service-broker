package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Slf4j
class ServiceInstanceRepositoryTest {

    @Value("${service.plan.standard.uuid}")
    private UUID servicePlanId;

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Test
    void testSave() throws InterruptedException, ExecutionException, JsonProcessingException {
        TopicServiceInstance topicServiceInstance = TopicServiceInstance.builder().topicName(UUID.randomUUID().toString()).uuid(UUID.randomUUID()).created(new Date()).build();
        serviceInstanceRepository.save(topicServiceInstance);
    }

    @Test
    void testSaveAndList() throws InterruptedException, ExecutionException, JsonProcessingException {
        UUID uuid = UUID.randomUUID();
        TopicServiceInstance topicServiceInstance = TopicServiceInstance.builder().topicName(UUID.randomUUID().toString()).uuid(uuid).created(new Date()).build();
        serviceInstanceRepository.save(topicServiceInstance);
        // wait at most 5 seconds for the data to be re-read from Kafka
        assertNotNull(waitFor(uuid, 50, 100));
    }

    /*
     * wait delay * iterations milliseconds for the service instance being picked up by the repository.
     */
    private TopicServiceInstance waitFor(UUID uuid, long delay, int iterations) throws InterruptedException {
        TopicServiceInstance stored = null;
        for (int i = 0; i < iterations; i++) {
            if (stored != null) break;
            Thread.sleep(delay);
            stored = serviceInstanceRepository.get(uuid);
        }
        return stored;
    }

    @Test
    void testSaveAndDelete() throws InterruptedException, ExecutionException, JsonProcessingException {
        UUID uuid = UUID.randomUUID();
        TopicServiceInstance topicServiceInstance = TopicServiceInstance.builder()
                .topicName(UUID.randomUUID().toString())
                .uuid(uuid)
                .created(new Date())
                .planId(servicePlanId)
                .build();

        serviceInstanceRepository.save(topicServiceInstance);
        assertNotNull(waitFor(uuid, 50, 100));
        serviceInstanceRepository.delete(uuid);
        assertNull(waitFor(uuid, 50, 100));
        serviceInstanceRepository.delete(uuid);
    }

}


