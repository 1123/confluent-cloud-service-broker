package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
@Slf4j
class ServiceInstanceRepository {

    @Autowired
    private ServiceInstanceCache serviceInstanceCache;

    @Autowired
    private KafkaProducer<String, String> kafkaProducer;

    @Value("${broker.store.topic.name}")
    private String serviceInstanceStoreTopic;

    private ObjectMapper objectMapper = new ObjectMapper();

    void save(TopicServiceInstance serviceInstance) throws JsonProcessingException, ExecutionException, InterruptedException {
        log.info("Saving service instance to Kafka.");
        Future<RecordMetadata> result = kafkaProducer.send(
                new ProducerRecord<>(
                        serviceInstanceStoreTopic,
                        serviceInstance.uuid.toString(),
                        objectMapper.writeValueAsString(serviceInstance)
                )
        );
        log.info(result.get().toString());
    }

    TopicServiceInstance get(UUID uuid) {
        return serviceInstanceCache.getInstances().get(uuid);
    }

    /**
     * Delete a service instance. This is done by publishing a tombstone message.
     * @param uuid: the id of the service instance to be deleted.
     */
    void delete(UUID uuid) throws ExecutionException, InterruptedException {
        Future<RecordMetadata> result = kafkaProducer.send(
                new ProducerRecord<>(
                        serviceInstanceStoreTopic,
                        uuid.toString(),
                        null
                )
        );
        log.info(result.get().toString());
    }
}
