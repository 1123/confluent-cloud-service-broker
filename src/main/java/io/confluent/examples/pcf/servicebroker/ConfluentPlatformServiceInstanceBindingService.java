package io.confluent.examples.pcf.servicebroker;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import reactor.core.publisher.Mono;

public class ConfluentPlatformServiceInstanceBindingService implements ServiceInstanceBindingService {

    public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        String user = (String) request.getParameters().get("user");
        String serviceInstanceId = request.getServiceInstanceId();
        // TODO: set ACLs
        return Mono.empty();
    }

}
