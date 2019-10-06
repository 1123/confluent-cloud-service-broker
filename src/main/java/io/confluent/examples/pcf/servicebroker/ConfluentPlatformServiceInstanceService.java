package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class ConfluentPlatformServiceInstanceService implements ServiceInstanceService {

    @Autowired
    private AdminClient adminClient;

    @Value( "${broker.store.topic.replication}" )
    private short replicationFactor;

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest) {
        log.info("Creating service instance.");
        String topic = (String) createServiceInstanceRequest.getParameters().get("topic_name");
        if (topic == null || topic.isEmpty()) {
            throw new RuntimeException("topic name is missing.");
        }
        try {
            CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(new NewTopic(topic, 1, replicationFactor)));
            result.all().get();
            serviceInstanceRepository.save(
                    TopicServiceInstance.builder()
                            .created(new Date())
                            .topicName(topic)
                            .uuid(UUID.fromString(createServiceInstanceRequest.getServiceInstanceId()))
                            .planId(UUID.fromString(createServiceInstanceRequest.getPlanId()))
                            .bindings(new ArrayList<>())
                            .build()
            );
        } catch (ExecutionException | InterruptedException | JsonProcessingException e) {
            log.warn(e.getMessage());
            throw new RuntimeException(e);
        }
        return Mono.just(
                CreateServiceInstanceResponse.builder()
                        .async(false)
                        .instanceExisted(false)
                        .build()
        );
    }

    public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
        TopicServiceInstance topicServiceInstance = serviceInstanceRepository.get(UUID.fromString(request.getServiceInstanceId()));
        GetServiceInstanceResponse response = GetServiceInstanceResponse.builder().parameters(Map.of("topic", topicServiceInstance.topicName)).build();
        return Mono.just(response);
    }

    @Override
    public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest) {
        try {
            serviceInstanceRepository.delete(UUID.fromString(deleteServiceInstanceRequest.getServiceInstanceId()));
            return Mono.just(DeleteServiceInstanceResponse.builder().build());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Mono.empty();
        }
    }
}
