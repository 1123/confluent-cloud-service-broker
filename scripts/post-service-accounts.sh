#!/bin/bash

set -u -e

curl \
  -u $REST_USER:$REST_PASSWORD \
  -X POST \
  -H "Content-Type: application/json" \
  -d '[
  {"apiKey":"'${SAMPLE_CLIENT_KEY}'", "apiSecret":"'${SAMPLE_CLIENT_SECRET}'", "serviceAccount":"'${SAMPLE_CLIENT_ACCOUNT}'"}, 
  {"apiKey":"'${SAMPLE_CLIENT_KEY}'", "apiSecret":"'${SAMPLE_CLIENT_SECRET}'", "serviceAccount":"'${SAMPLE_CLIENT_ACCOUNT}'"}, 
  {"apiKey":"'${SAMPLE_CLIENT_KEY}'", "apiSecret":"'${SAMPLE_CLIENT_SECRET}'", "serviceAccount":"'${SAMPLE_CLIENT_ACCOUNT}'"}
]' \
  $REST_API/accounts 

