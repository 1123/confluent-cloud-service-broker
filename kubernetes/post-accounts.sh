set -x
kubectl exec \
  -n catalog ccloud-service-broker \
  -- curl -X POST -H "Content-Type: application/json" -d '[ {"apiKey":"'${SAMPLE_CLIENT_KEY}'", "apiSecret":"'${SAMPLE_CLIENT_SECRET}'", "serviceAccount":"'${SAMPLE_CLIENT_ACCOUNT}'"}, {"apiKey":"'${SAMPLE_CLIENT_KEY}'", "apiSecret":"'${SAMPLE_CLIENT_SECRET}'", "serviceAccount":"'${SAMPLE_CLIENT_ACCOUNT}'"}, {"apiKey":"'${SAMPLE_CLIENT_KEY}'", "apiSecret":"'${SAMPLE_CLIENT_SECRET}'", "serviceAccount":"'${SAMPLE_CLIENT_ACCOUNT}'"} ]' http://$REST_USER:$REST_PASSWORD@localhost:8080/accounts
