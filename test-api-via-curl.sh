#!/bin/bash 
set -e -u

# Credentials are set in src/main/resources/application.yaml
CREDENTIALS=cloud-controller:13E663CA-1514-4278-A69D-84ACB4BF5B38

curl http://${CREDENTIALS}@localhost:8080/v2/catalog | jq .

serviceInstanceId=$(uuidgen)

curl -i http://${CREDENTIALS}@localhost:8080/v2/service_instances/$serviceInstanceId -d '{
  "context": {
    "platform": "cloudfoundry"
  },
  "service_id": "d439bf26-0d84-4482-b83a-028c568557c2",
  "plan_id": "4d066a85-d33f-440f-8930-f1e6fc611455",
  "organization_guid": "org-guid-here",
  "space_guid": "space-guid-here",
  "parameters": {
    "topic_name": "'$(uuidgen)'"
  }
}' -X PUT -H "X-Broker-API-Version: 2.12" -H "Content-Type: application/json"

sleep 1

curl -i http://${CREDENTIALS}@localhost:8080/v2/service_instances/$serviceInstanceId

sleep 1

curl \
  -i "http://${CREDENTIALS}@localhost:8080/v2/service_instances/${serviceInstanceId}?service_id=d439bf26-0d84-4482-b83a-028c568557c2&plan_id=4d066a85-d33f-440f-8930-f1e6fc611455" \
  -X DELETE \
  -H "X-Broker-API-Version: 2.12"

