cp ../target/kafka-service-broker-1.0-SNAPSHOT.jar .
gcloud builds submit --tag gcr.io/solutionsarchitect-01/ccloud-service-broker .
