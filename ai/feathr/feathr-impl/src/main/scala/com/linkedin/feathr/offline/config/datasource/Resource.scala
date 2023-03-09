package com.linkedin.feathr.offline.config.datasource

import com.fasterxml.jackson.annotation.JsonProperty

case class Resource(@JsonProperty("azure") azureResource: AzureResource,
                    @JsonProperty("aws") awsResource: AwsResource)

case class AzureResource(@JsonProperty("REDIS_PASSWORD") redisPassword: String,
                         @JsonProperty("REDIS_HOST") redisHost: String,
                         @JsonProperty("REDIS_PORT") redisPort: String,
                         @JsonProperty("REDIS_SSL_ENABLED") redisSslEnabled: String,
                         @JsonProperty("ADLS_ACCOUNT") adlsAccount: String,
                         @JsonProperty("ADLS_KEY") adlsKey: String,
                         @JsonProperty("BLOB_ACCOUNT") blobAccount: String,
                         @JsonProperty("BLOB_KEY") blobKey: String,
                         @JsonProperty("JDBC_TABLE") jdbcTable: String,
                         @JsonProperty("JDBC_USER") jdbcUser: String,
                         @JsonProperty("JDBC_PASSWORD") jdbcPassword: String,
                         @JsonProperty("JDBC_DRIVER") jdbcDriver: String,
                         @JsonProperty("JDBC_AUTH_FLAG") jdbcAuthFlag: String,
                         @JsonProperty("JDBC_TOKEN") jdbcToken: String)

case class AwsResource(@JsonProperty("S3_ENDPOINT") s3Endpoint: String,
                       @JsonProperty("S3_ACCESS_KEY") s3AccessKey: String,
                       @JsonProperty("S3_SECRET_KEY") s3SecretKey: String)

case class FeathrStoreConfig(@JsonProperty("resource")resource: Resource)