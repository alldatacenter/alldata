package com.linkedin.feathr.offline.config.location

import com.linkedin.feathr.common.Header
import com.linkedin.feathr.offline.generation.SparkIOUtils
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.hadoop.mapred.JobConf

case class PathList(paths: List[String]) extends DataLocation {
  override def getPath: String = paths.mkString(";")

  override def getPathList: List[String] = paths

  override def isFileBasedLocation(): Boolean = true

  override def loadDf(ss: SparkSession, dataIOParameters: Map[String, String] = Map()): DataFrame = {
    SparkIOUtils.createUnionDataFrame(getPathList, dataIOParameters, new JobConf(), List()) //TODO: Add handler support here. Currently there are deserilization issues with adding handlers to factory builder.
  }

  override def writeDf(ss: SparkSession, df: DataFrame, header: Option[Header]): Unit = ???

  override def toString: String = s"PathList(path=[${paths.mkString(",")}])"
}

object PathList {
  def apply(path: String): PathList = PathList(path.split(";").toList)
  def unapply(pathList: PathList): Option[List[String]] = Some(pathList.paths)
}
