package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
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
@Log
public class ServiceInstanceRepository {

    @Autowired
    private ServiceInstanceCache serviceInstanceCache;

    @Autowired
    private KafkaProducer<String, String> kafkaProducer;

    @Value("${service.instance.store.topic}")
    private String serviceInstanceStoreTopic;

    private ObjectMapper objectMapper = new ObjectMapper();

    void save(TopicServiceInstance serviceInstance) throws JsonProcessingException, ExecutionException, InterruptedException {
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

}
