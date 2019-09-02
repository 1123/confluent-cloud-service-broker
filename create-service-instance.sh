curl -i http://localhost:8080/v2/service_instances/foo?accepts_incomplete=true -d '{
  "context": {
    "platform": "cloudfoundry",
    "some_field": "some-contextual-data"
  },
  "service_id": "confluent-kafka",
  "plan_id": "standard",
  "organization_guid": "org-guid-here",
  "space_guid": "space-guid-here",
  "parameters": {
    "topic_name": "topic-e"
  }
}' -X PUT -H "X-Broker-API-Version: 2.12" -H "Content-Type: application/json"
