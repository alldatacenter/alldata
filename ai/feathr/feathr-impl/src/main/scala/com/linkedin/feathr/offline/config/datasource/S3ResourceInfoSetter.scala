package com.linkedin.feathr.offline.config.datasource

import org.apache.spark.sql.SparkSession

private[feathr] class S3ResourceInfoSetter extends ResourceInfoSetter() {
  val S3_ENDPOINT = "S3_ENDPOINT"
  val S3_ACCESS_KEY = "S3_ACCESS_KEY"
  val S3_SECRET_KEY = "S3_SECRET_KEY"

  override val params = List(S3_ENDPOINT, S3_ACCESS_KEY, S3_SECRET_KEY)

  override def setupHadoopConfig(ss: SparkSession, context: Option[DataSourceConfig], resource: Option[Resource]): Unit = {
    ss.sparkContext
      .hadoopConfiguration.set("fs.s3a.endpoint", getAuthStr(S3_ENDPOINT, context, resource))
    if (!getAuthStr(S3_ACCESS_KEY, context, resource).contains("None")) {
      ss.sparkContext
        .hadoopConfiguration.set("fs.s3a.access.key", getAuthStr(S3_ACCESS_KEY, context, resource))
    }
    if (!getAuthStr(S3_SECRET_KEY, context, resource).contains("None")) {
      ss.sparkContext
        .hadoopConfiguration.set("fs.s3a.secret.key", getAuthStr(S3_SECRET_KEY, context, resource))
    }
  }

  def getAuthFromConfig(str: String, resource: Resource): String = {
    str match {
      case S3_ENDPOINT => resource.awsResource.s3Endpoint
      case S3_ACCESS_KEY => resource.awsResource.s3AccessKey
      case S3_SECRET_KEY => resource.awsResource.s3SecretKey
      case _ => EMPTY_STRING
    }
  }
}

private[feathr] object S3ResourceInfoSetter{
  val sqlSetter = new S3ResourceInfoSetter()

  def setup(ss: SparkSession, config: DataSourceConfig, resource: Resource): Unit ={
    sqlSetter.setup(ss, config, resource)
  }
}