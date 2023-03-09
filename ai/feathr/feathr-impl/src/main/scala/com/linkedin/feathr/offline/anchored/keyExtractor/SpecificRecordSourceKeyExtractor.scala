package com.linkedin.feathr.offline.anchored.keyExtractor

import com.linkedin.feathr.common.AnchorExtractor
import com.linkedin.feathr.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import com.typesafe.config.ConfigRenderOptions
import org.apache.spark.sql._

/**
 *   This is the source key extractor class for user defined AnchorExtractor class
 * @param anchorExtractorV1
 */
private[feathr] class SpecificRecordSourceKeyExtractor(
    anchorExtractorV1: AnchorExtractor[Any],
    private val keyExprs: Seq[String] = Seq(),
    private val keyAlias: Option[Seq[String]] = None)
    extends SourceKeyExtractor {
  val JOIN_KEY_PREFIX = anchorExtractorV1.toString.replaceAll("[^\\w]", "") + "_"
  val MAX_KEY_FIELD_NUM = 5

  override def appendKeyColumns(dataFrame: DataFrame): DataFrame = {
    throw new FeathrException(ErrorLabel.FEATHR_ERROR, "appendKeyColumns function is not supported SpecificRecordSourceKeyExtractor")
  }

  def getKey(datum: Any): Seq[String] = {
    anchorExtractorV1.getKey(datum)
  }

  /**
   * Return the key column name of the current source, since appendKeyColumns is not supported by this source key
   * extractor (will special handle it), we just return place holders.
   * when the rdd is empty, pass None as datum, then this function
   * will return empty Seq to signal empty dataframe
   *
   * @param datum
   * @return
   */
  override def getKeyColumnNames(datum: Option[Any]): Seq[String] = {
    if (datum.isDefined) {
      val size = anchorExtractorV1.getKey(datum.get).size
      (1 to size).map(JOIN_KEY_PREFIX + _)
    } else {
      Seq()
    }
  }

  override def getKeyColumnAlias(datum: Option[Any]): Seq[String] = {
    keyAlias.getOrElse(keyExprs)
  }

  override def toString(): String =
    super.toString() + anchorExtractorV1.getClass.getCanonicalName +
      " withParams:" + params.map(_.root().render(ConfigRenderOptions.concise()).mkString(","))
}
