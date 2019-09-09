package io.confluent.examples.pcf.servicebroker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopicServiceInstance {

    UUID uuid;
    String topicName;
    Date created;

}
