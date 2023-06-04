#### InLong DataProxy docker image
InLong DataProxy is available for development and experience.

##### Pull Image
```
docker pull inlong/dataproxy:1.7.0
```

##### Start Container
```
docker run -d --name dataproxy \
-p 46801:46801 \
-e MANAGER_OPENAPI_IP=manager_openapi_ip \
-e MANAGER_OPENAPI_PORT=manager_openapi_port \
-e TUBMQ_MASTER_LIST=tube_master_address inlong/dataproxy
```