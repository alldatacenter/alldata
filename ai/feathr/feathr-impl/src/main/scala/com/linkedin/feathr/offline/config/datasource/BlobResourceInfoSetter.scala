package com.linkedin.feathr.offline.config.datasource

import org.apache.spark.sql.SparkSession

private[feathr] class BlobResourceInfoSetter extends ResourceInfoSetter() {
  val BLOB_ACCOUNT = "BLOB_ACCOUNT"
  val BLOB_KEY = "BLOB_KEY"

  override val params = List(BLOB_ACCOUNT, BLOB_KEY)

  override def setupHadoopConfig(ss: SparkSession, context: Option[DataSourceConfig] = None, resource: Option[Resource] = None): Unit = {
    val blobParam = s"fs.azure.account.key.${getAuthStr(BLOB_ACCOUNT, context, resource)}.blob.core.windows.net"
    val blobKey = getAuthStr(BLOB_KEY, context, resource)

    ss.sparkContext
      .hadoopConfiguration.set(blobParam, blobKey)
  }

  def getAuthFromConfig(str: String, resource: Resource): String = {
    str match {
      case BLOB_ACCOUNT => resource.azureResource.blobAccount
      case BLOB_KEY => resource.azureResource.blobKey
      case _ => EMPTY_STRING
    }
  }
}

private[feathr] object BlobResourceInfoSetter{
  val blobSetter = new BlobResourceInfoSetter()

  def setup(ss: SparkSession, config: DataSourceConfig, resource: Resource): Unit ={
    blobSetter.setup(ss, config, resource)
  }
}