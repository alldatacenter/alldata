{
  "redis": {
    "tsp": {
      "host": "${REDIS_ENDPOINT}",
      "port": 6379,
      "password": "${REDIS_PASSWORD}",
      "db": 1
    },
    "celery_result_backend": {
      "host": "${REDIS_ENDPOINT}",
      "port": 6379,
      "password": "${REDIS_PASSWORD}",
      "db": 2
    },
    "celery_broker": {
      "host": "${REDIS_ENDPOINT}",
      "port": 6379,
      "password": "${REDIS_PASSWORD}",
      "db": 2
    }
  },
  "mysql": {
    "aiops": {
      "host": "${DATA_DB_HOST}",
      "port": ${DATA_DB_PORT},
      "username": "${DATA_DB_USER}",
      "password": "${DATA_DB_PASSWORD}",
      "db": "${DATA_DB_AIOPS_NAME}"
    },
    "pmdb": {
      "host": "${DATA_DB_HOST}",
      "port": ${DATA_DB_PORT},
      "username": "${DATA_DB_USER}",
      "password": "${DATA_DB_PASSWORD}",
      "db": "${DATA_DB_PMDB_NAME}"
    }
  }
}