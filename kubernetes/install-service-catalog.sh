#!/bin/bash

set -u
set -e 

# following the instructions here: https://kubernetes.io/docs/tasks/service-catalog/install-service-catalog-using-helm/

helm repo add svc-cat https://svc-catalog-charts.storage.googleapis.com
kubectl create namespace catalog
helm install catalog svc-cat/catalog --namespace catalog
