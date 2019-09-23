#!/bin/bash

set -e
set -u

kafka-topics \
   --bootstrap-server localhost:10091 \
   --command-config client.config \
   --delete \
   --topic kafka.service.broker.service-instances \

 kafka-topics \
   --bootstrap-server localhost:10091 \
   --command-config client.config \
   --create \
   --topic kafka.service.broker.service-instances \
   --partitions 5 \
   --replication-factor 1
