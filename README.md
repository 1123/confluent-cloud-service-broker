# Overview

This is a Cloudfoundry service broker for provisioning and giving access to Kafka topics on a preprovisioned Kafka cluster. 
Registering this service broker with a Kubernetes Service Catalog has not been tested yet. 
Currently the only tested authentication method to the Kafka Cluster is SASL plain. 
Adding support for other authentication mechanisms should only require changes to configuration. 
This service broker has been validated to work with Confluent Cloud. 

The service broker is meant to give access to a central multitenant cluster and provides the following functionality:

* the 'cf create service' expects a topic name as parameter and will create the topic via the Kafka admin client API.
* the 'cf bind service' command expects the following parameters: 
  * a user name: The service broker will use this to create ACLs on the topic, such that the user has the rights to write and read from it.
  * a consumer group: The service broker will make sure that the user will have the rights to manage this consumer group on the broker.  

## Prerequisites

* Java 11 or later
* maven 
* access to maven central for downloading dependencies
* a Cloud Foundry installation for registering the service broker
* docker and docker-compose when trying out the application locally 
* access to confluent docker images when running locally
* When connecting to Confluent Cloud, a API Key and API Secret

## Testing

For integration testing a local Zookeeper server and Kafka broker are started. 

* running the tests is as simple as `mvn clean test`.

## Connecting to Confluent Cloud

* Insert your api key and secret into `application-ccloud.properties`.
* run your the service broker with the ccloud spring profile: `export SPRING_PROFILES_ACTIVE=ccloud; mvn spring-boot:run`

## Running locally

* You need access to a Kafka cluster with SASL Plain authentication mechanism enabled. You can get this by running `docker-compose up`. 
* Adjust the configuration in `src/main/resources/application.properties`. 
* Run `mvn spring-boot:run`

## Running on CloudFoundry

* Adjust manifest-pcf-dev.yaml to your needs and copy to manifest.yml
* Push to cloudfoundry `cf push -f manifest.yml`.
* register the service broker with cloudfoundry `cf create-service-broker kafka-broker <user> <password> http://kafka-service-broker.dev.cfdev.sh`
* Enable service access: `cf enable-service-access confluent-kafka`
* Try out creating a topic: `cf create-service confluent-kafka gold my-topic -c '{ "topic_name" : "gold-topic" }'`
* Bind to an application: `cf bind-service kafka-service-broker my-topic -c '{ "user": "User:bob", "consumer_group" : "consumer_group_1" }'`

