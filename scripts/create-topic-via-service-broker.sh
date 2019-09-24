set -e
set -u

topicName=$1
curl -X PUT localhost:8080/v2/service_instances/38c919ef-43d6-4b3e-ba31-988b753a9373 -d '{ "service_id": "d439bf26-0d84-4482-b83a-028c568557c2", "plan_id": "4d066a85-d33f-440f-8930-f1e6fc611455", "parameters": { "topic_name" : "'$topicName'" } }' -H "X-Broker-API-Version: 2.12" -H "Content-Type: application/json"
