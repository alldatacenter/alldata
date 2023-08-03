package com.linkedin.feathr.offline.config.location

import com.fasterxml.jackson.module.caseclass.annotation.CaseClassDeserialize
import com.linkedin.feathr.common.Header
import com.linkedin.feathr.offline.generation.SparkIOUtils
import org.apache.hadoop.mapred.JobConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.codehaus.jackson.annotate.JsonProperty

@CaseClassDeserialize()
case class SimplePath(@JsonProperty("path") path: String) extends DataLocation {
  override def loadDf(ss: SparkSession, dataIOParameters: Map[String, String] = Map()): DataFrame = {
    SparkIOUtils.createUnionDataFrame(getPathList, dataIOParameters, new JobConf(), List()) // The simple path is not responsible for handling custom data loaders.
  }

  override def writeDf(ss: SparkSession, df: DataFrame, header: Option[Header]): Unit = ???

  override def getPath: String = path

  override def getPathList: List[String] = List(path)

  override def isFileBasedLocation(): Boolean = {
    if (path.startsWith("jdbc:")) {
      false
    } else {
      true
    }
  }

  override def toString: String = s"SimplePath(path=${path})"
}
