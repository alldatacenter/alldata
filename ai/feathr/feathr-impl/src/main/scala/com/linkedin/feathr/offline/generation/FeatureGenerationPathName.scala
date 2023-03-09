package com.linkedin.feathr.offline.generation

// class holds the output path for feature generation
private[offline] object FeatureGenerationPathName {
  val FEATURES: String = "features"

  // path to store meta, data and schema
  final val metaFolder = "meta"
  final val dataFolder = "daily"
  final val schemaFolder = "schema"

  val STORE_NAME = "storeName"

  /**
   * get the path to store output dataset
   * @param parentPath base path (user provided in the output processor)
   * @param endTimeOpt end time as a string, yyyy/MM/dd
   * @return path to store output dataset
   */
  def getDataPath(parentPath: String, endTimeOpt: Option[String]): String = {
    parentPath + "/" + dataFolder + "/" + endTimeOpt.getOrElse("")
  }

  /**
   * get the path to store metadata/header
   * @param parentPath base path (user provided in the output processor)
   * @param endTimeOpt end time as a string, yyyy/MM/dd
   * @return path to store metadata/header
   */
  def getMetaPath(parentPath: String, endTimeOpt: Option[String]): String = {
    parentPath + "/" + metaFolder + "/" + endTimeOpt.getOrElse("")
  }

  /**
   * get the path to store redis schema
   * @param parentPath base path (user provided in the output processor)
   * @param endTimeOpt end time as a string, yyyy/MM/dd
   * @return path to store redis schema
   */
  def getSchemaPath(parentPath: String, endTimeOpt: Option[String]): String = {
    parentPath + "/" + schemaFolder + "/" + endTimeOpt.getOrElse("")
  }
}
