package io.confluent.examples.pcf.servicebroker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class TopicServiceInstance {

    public TopicServiceInstance() {
        this.bindings = new ArrayList<>();
    }

    UUID uuid;
    String topicName;
    Date created;
    UUID planId;
    List<TopicUserBinding> bindings;

}

