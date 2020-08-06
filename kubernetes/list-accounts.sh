set -x
kubectl exec \
  -n catalog ccloud-service-broker \
  -- curl http://$REST_USER:$REST_PASSWORD@localhost:8080/accounts
