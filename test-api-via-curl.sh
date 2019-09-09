#!/bin/bash 
set -e -u

serviceInstanceId=$(uuidgen)

curl -i http://localhost:8080/v2/service_instances/$serviceInstanceId -d '{
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

curl -i http://localhost:8080/v2/service_instances/$serviceInstanceId

sleep 1

curl -i "http://localhost:8080/v2/service_instances/${serviceInstanceId}?service_id=d439bf26-0d84-4482-b83a-028c568557c2&plan_id=4d066a85-d33f-440f-8930-f1e6fc611455" -X DELETE -H "X-Broker-API-Version: 2.12"

