package io.confluent.examples.pcf.servicebroker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

@Service
public class ConfluentPlatformServiceInstanceService implements ServiceInstanceService {

    @Autowired
    private TopicCreator topicCreator;

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest) {
        String topic = (String) createServiceInstanceRequest.getParameters().get("topic_name");
        if (topic == null || topic.isEmpty()) {
            throw new RuntimeException("topic name is missing.");
        }
        try {
            topicCreator.create(topic, 3, (short) 3);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return Mono.just(
                CreateServiceInstanceResponse.builder()
                        .async(false)
                        .instanceExisted(false)
                        .build()
        );
    }

    @Override
    public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest) {
        return null;
    }
}
