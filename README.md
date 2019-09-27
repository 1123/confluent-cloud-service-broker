# Overview

This is a Cloudfoundry service broker for provisioning and giving access to Kafka topics on a preprovisioned Kafka cluster. Currently the only supported authentication method to the Kafka Cluster is SASL plain. Adding support for other authentication mechanisms should only require changes to configuration. 

Specifically, 

* the 'cf create service' expects a topic name as parameter and will create the topic via the Kafka admin client API.
* the 'cf bind service' command expects a user name and will create ACLs, such that the user has the rights to write and read from the topic, that the application is bound to.

## Prerequisites

* a recent version of Java for building
* maven 
* access confluent docker images for integration testing
* docker-compose for integration testing
* a cloud foundry installation for registering the service broker

## Testing

* run `docker-compose up` for bringing up a single-node cluster with SASL Plain authentication setup. 
* wait until both containers have come up. 
* run the integration tests using `mvn clean test`. 
* run `docker-compose down` for cleaning up your environment. 

## Running locally

* You need access to a Kafka cluster. E.g. by running `docker-compose up` in the root directory of this repository.
* Adjust the configuration in `src/main/resources/application.properties`. 
* Run `mvn spring-boot:run`

