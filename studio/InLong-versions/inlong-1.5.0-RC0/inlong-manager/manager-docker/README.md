## InLong Manager docker image

InLong Manager is available for development and experience.

### Pull Image

```
docker pull inlong/manager:latest
```

### Start Container

```
docker run -d --name manager -p 8083:8083 \
-e ACTIVE_PROFILE=prod \
-e JDBC_URL=127.0.0.1:3306 \
-e USERNAME=root \
-e PASSWORD=inlong \
inlong/manager:latest
```