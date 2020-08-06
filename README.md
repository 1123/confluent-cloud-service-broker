# Confluent Cloud Shared Service Broker

This is a Kubernetes and Cloud Foundry service broker for provisioning and granting access to Kafka topics on Confluent Cloud, or for a dedicated Kafka Cluster. It is written according to the Open Service Broker API: https://www.openservicebrokerapi.org .  
It does not provision Kafka clusters itself. 

The service broker grants access to a central multi-tenant cluster and provides the following functionality:

* The concept of <em>service instance</em> in the Open Service Broker API maps to a Kafka topic. 
* The concept of <em>service instance binding</em> in the Open Service Broker API maps to the following concepts in Kubernetes and CloudFoundry:
  * Creation of a secret object holding credentials for accessing the topic in Kubernetes. 
    This secret can be referenced by environment variables in the bound app, or mounted as a volume just as other Kubernetes secrets.  
  * Injection of credentials for accessing the topic into the VCAP_SERVICES` environment variable of the app when called from CloudFoundry.   

## Prerequisites

* Java 11 or later
* maven 
* access to maven central for downloading dependencies
* a Kubernetes or Cloud Foundry installation for registering the service broker. Google Kubernetes Service or Pivotal Web Services are easy to use for getting started. 
* A Confluent Cloud API Key and API with permissions to create topics. 

## Testing

For integration testing a local Zookeeper server and Kafka broker are started. 

* running the tests is as simple as `mvn clean test`.

## Running locally

* You need access to a Kafka cluster with SASL Plain authentication mechanism enabled. You can get this by running `docker-compose up`. 
* Adjust the configuration in `src/main/resources/application.properties`. 
* Run `mvn spring-boot:run`

## Running on Kubernetes

See the `kubernetes` subdirectory for installation Kubernetes. This has been tested with Google Kubernetes Service.
The steps are as follows:

1. Copy `src/main/resources/application-ccloud.yaml` to `src/main/resources/<your-name>.yaml` and adjust the credentials for accessing Confluent Cloud.
2. build the project: `mvn clean package`
3. copy the resulting jar to the kubernetes subdirectory: `cp target/kafka-service-broker-1.0-SNAPSHOT.jar kubernetes/`
4. Edit `kubernetes/Dockerfile` and set the environment variable `SPRING_PROFILES_ACTIVE to <your-name>
5. build the image: `cd kubernetes`;  `docker build .`
6. push the image to a container registry that can be accessed by your Kubernetes cluster. If using Google Kubernetes service, you can use the `build.sh` script for this. 
7. Make sure the namespace `catalog` exists. Deploy the service-broker `kubectl apply -f service-broker.yaml`.
8. install the service catalog API extension: `install-service-catalog.sh`
9. create a kubernetes service object for accessing the service broker: `kubectl apply -f service-broker-service.yaml`
10. Register the service broker with Kubernetes: `kubectl apply -f service-broker-registration.yaml`
11. Create one or more Confluent Cloud service accounts and associated api keys via the `ccloud` cli. Post these service accounts to the service broker, such that it can supply them to client applications for accessing the topics. See the script `post-accounts.sh` for details. 
12. Create a topic via the service broker: `kubectl apply -f service-instance.yaml`
13. Bind the topic: `kubectl apply -f service-binding.yaml`. This will create the kubernetes secret object that can be  referenced from your Confluent Cloud client application. 


## Running on Cloud Foundry

1. Adjust manifest-pcf-dev.yaml to your needs and copy to manifest.yml
1. Push to cloud foundry `cf push -f manifest.yml`.
1. register the service broker with cloud foundry `cf create-service-broker kafka-broker <user> <password> http://kafka-service-broker.dev.cfdev.sh`
1. Enable service access: `cf enable-service-access confluent-kafka`
1. Create one or more Confluent Cloud service accounts and associated api keys via the `ccloud` cli. Post these service accounts to the service broker, such that it can supply them to client applications for accessing the topics. See the script `post-accounts.sh` for details. 
1. Try out creating a topic: `cf create-service confluent-kafka gold my-topic -c '{ "topic_name" : "gold-topic" }'`
1. Bind to an application: `cf bind-service kafka-service-broker my-topic -c '{ "consumer_group" : "consumer_group_1" }'`

