package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ConfluentPlatformServiceInstanceBindingService implements ServiceInstanceBindingService {

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private AdminClient adminClient;

    public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        String user = (String) request.getParameters().get("user");
        if (user == null) throw new RuntimeException("User must be specified in bind service request. ");
        UUID serviceInstanceId = UUID.fromString(request.getServiceInstanceId());
        TopicServiceInstance topicServiceInstance = serviceInstanceRepository.get(serviceInstanceId);
        createAcls(topicServiceInstance.topicName, user);
        addBinding(topicServiceInstance, request.getBindResource().getAppGuid(), request.getBindingId(), user);
        // TODO: check if binding existed.
        return Mono.just(CreateServiceInstanceAppBindingResponse.builder().async(false).bindingExisted(false).build());
    }

    private void createAcls(String topicName, String user) {
        CreateAclsResult createAclsResult = adminClient.createAcls(
                Collections.singletonList(
                        // TODO: AclOperation.ALL is probably too much.
                        new AclBinding(
                                new ResourcePattern(ResourceType.TOPIC, topicName, PatternType.LITERAL),
                                new AccessControlEntry(user, "*", AclOperation.ALL, AclPermissionType.ALLOW)
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
        Optional<TopicUserBinding> binding =
                topicServiceInstance.getBindings().stream().filter(b -> b.id.equals(request.getBindingId())).findFirst();
        if (! binding.isPresent()) {
            throw new RuntimeException("No such binding. ");
        }
        removeAcls(topicServiceInstance, request, binding.get());
        try {
            removeBinding(topicServiceInstance, request, binding.get());
        } catch (InterruptedException | ExecutionException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return Mono.just(DeleteServiceInstanceBindingResponse.builder().build());
    }

    private void removeAcls(TopicServiceInstance topicServiceInstance, DeleteServiceInstanceBindingRequest request, TopicUserBinding binding) {
        adminClient.deleteAcls(
                Collections.singleton(
                        new AclBindingFilter(
                                new ResourcePatternFilter(ResourceType.TOPIC, topicServiceInstance.topicName, PatternType.LITERAL),
                                new AccessControlEntryFilter(binding.user, "*", AclOperation.ALL, AclPermissionType.ALLOW)
                        )
                )
        );
    }

    private void removeBinding(TopicServiceInstance topicServiceInstance, DeleteServiceInstanceBindingRequest request, TopicUserBinding binding) throws InterruptedException, ExecutionException, JsonProcessingException {
        List<TopicUserBinding> remainingBindings =
                topicServiceInstance.getBindings().stream().filter(
                        b -> !b.id.equals(request.getBindingId())).collect(Collectors.toList()
                );
        topicServiceInstance.setBindings(remainingBindings);
        serviceInstanceRepository.save(topicServiceInstance);
    }

}
