## Docker and Kubernetes for InLong

Requirements:
- [Docker](https://docs.docker.com/engine/install/) 19.03.1+

### Build Images

```shell
mvn clean package -DskipTests -Pdocker
```

### Run All Modules

- [docker-compose](docker-compose/README.md)
- [kubernetes](kubernetes/README.md)
