# metric datasource config
metric.datasource.driver-class-name=com.mysql.jdbc.Driver
metric.datasource.url=jdbc:mysql://${DATA_DB_HOST}:${DATA_DB_PORT}/${DATA_DB_HEALTH_NAME}?useUnicode=true&characterEncoding=utf-8&useSSL=false
metric.datasource.database=${DATA_DB_HEALTH_NAME}
metric.datasource.username=${DATA_DB_USER}
metric.datasource.password=${DATA_DB_PASSWORD}

health.instance.url=http://${HEALTH_ENDPOINT}

default.minio.url=http://${MINIO_ENDPOINT}
default.minio.access_key=${MINIO_ACCESS_KEY}
default.minio.secret_key=${MINIO_SECRET_KEY}
default.minio.rules_bucket=metric-rules
default.minio.rules_file=sreworks/metric/rules.json
