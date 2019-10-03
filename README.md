# Overview

This is a Cloudfoundry service broker for provisioning and giving access to Kafka topics on a preprovisioned Kafka cluster. 
Registering this service broker with a Kubernetes Service Catalog has not been tested yet. 
Currently the only tested authentication method to the Kafka Cluster is SASL plain. 
Adding support for other authentication mechanisms should only require changes to configuration. 

The service broker works as follows:

* the 'cf create service' expects a topic name as parameter and will create the topic via the Kafka admin client API.
* the 'cf bind service' command expects the following parameters: 
  * a user name: The service broker will use this to create ACLs on the topic, such that the user has the rights to write and read from it.
  * a consumer group: The service broker will make sure that the user will have the rights to manage this consumer group on the broker.  

## Prerequisites

* a recent version of Java for building
* maven 
* access to maven central for downloadnig dependencies
* a cloud foundry installation for registering the service broker
* docker and docker-compose when running the application locally
* access to confluent images when running locally

## Testing

For integration testing a local Zookeeper server and Kafka broker are started. 

* running the tests is as simple as `mvn clean test`.

## Running locally

* You need access to a Kafka cluster with SASL Plain authentication mechanism enabled. You can get this by running `docker-compose up`. 
* Adjust the configuration in `src/main/resources/application.properties`. 
* Run `mvn spring-boot:run`

