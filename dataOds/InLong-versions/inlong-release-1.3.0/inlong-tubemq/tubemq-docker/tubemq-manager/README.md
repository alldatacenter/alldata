#### tubemq-manager docker image
TubeMQ manager is available for development and experience.

##### Pull Image
```
docker pull inlong/tubemq-manager:latest
```

##### Start Container
- start MySQL 5.7+
- start TubeMQ Server
- run Container

```
docker run -d --name manager -p 8089:8089 \
-e MYSQL_HOST=127.0.0.1 -e MYSQL_USER=root -e MYSQL_PASSWD=inlong \
-e TUBE_MASTER_IP=127.0.0.1 -e TUBE_MASTER_PORT=8715 \
-e TUBE_MASTER_WEB_PORT=8080 -e TUBE_MASTER_TOKEN=abc \
inlong/tubemq-manager
```