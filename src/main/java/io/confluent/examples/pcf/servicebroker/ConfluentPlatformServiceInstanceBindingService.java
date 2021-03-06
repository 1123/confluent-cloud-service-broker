package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.examples.pcf.servicebroker.accounts.ServiceAccountAndApiKey;
import io.confluent.examples.pcf.servicebroker.accounts.ServiceAccountAndApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConfluentPlatformServiceInstanceBindingService implements ServiceInstanceBindingService {

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private AdminClient adminClient;

    @Value("${broker.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Autowired
    private ServiceAccountAndApiKeyService serviceAccountAndApiKeyService;

    public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        String consumerGroup = (String) request.getParameters().get("consumer_group");
        if (consumerGroup == null || consumerGroup.equals("")) throw new RuntimeException("Consumer group must be specified in binding request. ");
        UUID serviceInstanceId = UUID.fromString(request.getServiceInstanceId());
        TopicServiceInstance topicServiceInstance = serviceInstanceRepository.get(serviceInstanceId);
        ServiceAccountAndApiKey serviceAccountAndApiKey = serviceAccountAndApiKeyService.get();
        createAcls(
                topicServiceInstance.topicName,
                "User:" + serviceAccountAndApiKey.getServiceAccount(),
                consumerGroup
        );
        addBinding(topicServiceInstance, request.getBindResource().getAppGuid(), request.getBindingId(), serviceAccountAndApiKey.getApiKey());
        // TODO: check if binding existed.
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("url", kafkaBootstrapServers);
        credentials.put("user", serviceAccountAndApiKey.getApiKey());
        credentials.put("password", serviceAccountAndApiKey.getApiSecret());
        credentials.put("serviceAccount", serviceAccountAndApiKey.getServiceAccount());
        credentials.put("topic", topicServiceInstance.topicName);
        credentials.put("consumer_group", consumerGroup);
        return Mono.just(
                CreateServiceInstanceAppBindingResponse.builder()
                        .credentials(credentials)
                        .async(false)
                        .bindingExisted(false)
                        .build()
        );
    }

    private void createAcls(String topicName, String serviceAccount, String consumerGroup) {
        CreateAclsResult createAclsResult = adminClient.createAcls(
                Arrays.asList(
                        // TODO: AclOperation.ALL is probably too much.
                        new AclBinding(
                                new ResourcePattern(ResourceType.TOPIC, topicName, PatternType.LITERAL),
                                new AccessControlEntry(serviceAccount, "*", AclOperation.ALL, AclPermissionType.ALLOW)
                        ),
                        new AclBinding(
                                new ResourcePattern(ResourceType.GROUP, consumerGroup, PatternType.LITERAL),
                                new AccessControlEntry(serviceAccount, "*", AclOperation.ALL, AclPermissionType.ALLOW)
                        )
                )
        );
        try {
            createAclsResult.all().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void addBinding(TopicServiceInstance topicServiceInstance, String appGuid, String bindingId, String user) {
        topicServiceInstance.bindings.add(
                TopicUserBinding.builder()
                        .id(bindingId)
                        .app(appGuid)
                        .user(user)
                        .build()
        );
        try {
            serviceInstanceRepository.save(topicServiceInstance);
        } catch (JsonProcessingException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
        TopicServiceInstance topicServiceInstance = serviceInstanceRepository.get(UUID.fromString(request.getServiceInstanceId()));
        if (topicServiceInstance == null) {
            log.error("could not find service instance for UUID" + request.getServiceInstanceId());
            return Mono.just(DeleteServiceInstanceBindingResponse.builder().build());
        }
        Optional<TopicUserBinding> binding =
                topicServiceInstance.getBindings().stream().filter(b -> b.id.equals(request.getBindingId())).findFirst();
        if (binding.isEmpty()) {
            log.error("No such binding. ");
            return Mono.just(DeleteServiceInstanceBindingResponse.builder().build());
        }
        removeAcls(topicServiceInstance, binding.get());
        try {
            removeBinding(topicServiceInstance, request);
        } catch (InterruptedException | ExecutionException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return Mono.just(DeleteServiceInstanceBindingResponse.builder().build());
    }

    private void removeAcls(TopicServiceInstance topicServiceInstance, TopicUserBinding binding) {
        adminClient.deleteAcls(
                Collections.singleton(
                        new AclBindingFilter(
                                new ResourcePatternFilter(
                                        ResourceType.TOPIC,
                                        topicServiceInstance.topicName,
                                        PatternType.LITERAL
                                ),
                                new AccessControlEntryFilter(
                                        binding.user,
                                        "*",
                                        AclOperation.ALL,
                                        AclPermissionType.ALLOW
                                )
                        )
                )
        );
    }

    private void removeBinding(
            TopicServiceInstance topicServiceInstance,
            DeleteServiceInstanceBindingRequest request
    ) throws InterruptedException, ExecutionException, JsonProcessingException {
        List<TopicUserBinding> remainingBindings =
                topicServiceInstance.getBindings().stream().filter(
                        b -> !b.id.equals(request.getBindingId())).collect(Collectors.toList()
                );
        topicServiceInstance.setBindings(remainingBindings);
        serviceInstanceRepository.save(topicServiceInstance);
    }

}
