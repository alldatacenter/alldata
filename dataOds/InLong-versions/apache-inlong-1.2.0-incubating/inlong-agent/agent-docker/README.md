#### InLong Agent docker image
InLong Agent is available for development and experience.

##### Pull Image
```
docker pull inlong/agent:1.2.0-incubating
```

##### Start Container
```
docker run -d --name agent  -p 8008:8008 \
-e MANAGER_OPENAPI_IP=manager_opeapi_ip -e DATAPROXY_IP=dataproxy_ip \
-e MANAGER_OPENAPI_PORT=8082 -e DATAPROXY_PORT=46801 inlong/agent
```