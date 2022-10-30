## InLong Standalone Using Docker Compose

Deploy all InLong module by Docker Compose, it's only available for development.

Requirements:

- [Docker](https://docs.docker.com/engine/install/) 19.03.1+
- Docker Compose 1.29.2+

### Deploy

Manually copy SQL files from `inlong-manager/sql` and `inlong-audit/sql` to the `docker/docker-compose/sql` directory.

```shell
cp inlong-manager/sql/apache_inlong_manager.sql docker/docker-compose/sql
cp inlong-audit/sql/apache_inlong_audit.sql docker/docker-compose/sql
```

Then, start all components.

```shell
docker-compose up -d
```

### Use InLong

After all containers run successfully, you can access `http://localhost` with default account:

```shell
User: admin
Password: inlong
```

### Destroy

```shell
docker-compose down
```
