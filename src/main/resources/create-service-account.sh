ccloud login
serviceAccount=sb-$(date +%s)
ccloud kafka cluster use $KAKFA_CLUSTER_ID
ccloud service-account create $serviceAccount --description "service account 1"
ccloud 
