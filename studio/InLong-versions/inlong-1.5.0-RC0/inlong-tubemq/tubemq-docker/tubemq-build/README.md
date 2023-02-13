### Docker image for building TubeMQ
##### Pull Image
```
docker pull inlong/tubemq-build
```

#### Build TubeMQ
```
docker run -v REPLACE_WITH_SOURCE_PATH:/tubemq  inlong/tubemq-build clean package -DskipTests
```