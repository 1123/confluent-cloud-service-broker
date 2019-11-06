package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class ConfluentPlatformServiceInstanceService implements ServiceInstanceService {

    private AdminClient adminClient;
    // TODO: we should differentiate between the replication factor of the topics that we create, and the one of the storage topic.
    // TODO: What about min.in.sync.replicas?
    private short replicationFactor;
    private ServiceInstanceRepository serviceInstanceRepository;
    private Map<String, Integer> planToPartitionsMapping;

    public ConfluentPlatformServiceInstanceService(
            @Autowired AdminClient adminClient,
            @Value( "${broker.store.topic.replication}" ) short replicationFactor,
            @Autowired ServiceInstanceRepository serviceInstanceRepository,
            @Autowired Catalog catalog
            ) {
        this.adminClient = adminClient;
        this.replicationFactor = replicationFactor;
        this.serviceInstanceRepository = serviceInstanceRepository;
        planToPartitionsMapping = new HashMap<>();
        List<Plan> plans = catalog.getServiceDefinitions().get(0).getPlans();
        // TODO: this should be part of the configuration, rather than hard coded.
        planToPartitionsMapping.put(plans.get(0).getName(), 1);
        planToPartitionsMapping.put(plans.get(1).getName(), 6);
        planToPartitionsMapping.put(plans.get(2).getName(), 15);
    }

    private Integer getPartitions(CreateServiceInstanceRequest createServiceInstanceRequest) {
        Integer partitions = planToPartitionsMapping.get(createServiceInstanceRequest.getPlan().getName());
        if (partitions == null) {
            throw new RuntimeException(
                    "Number of partitions has not been configured for plan " +
                            createServiceInstanceRequest.getPlan().getName()
            );
        }
        return partitions;
    }

    private void createTopicAndStoreServiceInstance(
            CreateServiceInstanceRequest createServiceInstanceRequest,
            String topic
    ) throws InterruptedException, ExecutionException, JsonProcessingException {
        CreateTopicsResult result = adminClient.createTopics(
                Collections.singletonList(
                        new NewTopic(topic,
                                getPartitions(createServiceInstanceRequest),
                                replicationFactor)
                )
        );
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
    }

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest) {
        log.info("Creating service instance.");
        String topic = (String) createServiceInstanceRequest.getParameters().get("topic_name");
        if (topic == null || topic.isEmpty()) {
            throw new RuntimeException("topic name is missing.");
        }
        try {
            createTopicAndStoreServiceInstance(createServiceInstanceRequest, topic);
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
        log.info("Deleting service instance with id {}", deleteServiceInstanceRequest.getServiceInstanceId());
        TopicServiceInstance instance = serviceInstanceRepository.get(UUID.fromString(deleteServiceInstanceRequest.getServiceInstanceId()));
        if (instance == null) {
            log.info("Service instance not found");
            throw new ServiceInstanceDoesNotExistException(deleteServiceInstanceRequest.getServiceInstanceId());
        }
        deleteTopic(instance.topicName);
        try {
            serviceInstanceRepository.delete(UUID.fromString(deleteServiceInstanceRequest.getServiceInstanceId()));
            return Mono.just(DeleteServiceInstanceResponse.builder().build());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Mono.empty();
        }
    }

    void deleteTopic(String topicName) {
        try {
            DeleteTopicsResult deleteTopicsResult = adminClient.deleteTopics(Collections.singleton(UUID.randomUUID().toString()));
            deleteTopicsResult.all().get();
        } catch(ExecutionException e) {
            // if the topic is not there, then a previous delete call may have succeeded.
            log.info("No such topic");
        } catch (InterruptedException e) {
            throw new ServiceBrokerException("Interrupted while trying to delete the topic");
        }
        adminClient.deleteTopics(Collections.singleton(topicName));
    }
}
