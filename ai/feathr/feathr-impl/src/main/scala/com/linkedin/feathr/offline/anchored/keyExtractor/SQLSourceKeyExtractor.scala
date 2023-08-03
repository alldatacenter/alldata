package com.linkedin.feathr.offline.anchored.keyExtractor

import com.linkedin.feathr.offline.util.AnchorUtils.removeNonAlphaNumChars
import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import com.linkedin.feathr.swj.LateralViewParams
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * This is the source key extractor class for SQL based anchor features
 *
 * @param keyExprs
 */
class SQLSourceKeyExtractor(
    val keyExprs: Seq[String],
    private val keyAlias: Option[Seq[String]] = None,
    private val lateralView: Option[LateralViewParams] = None)
    extends SourceKeyExtractor {

  override def getKeyColumnNames(datum: Option[Any]): Seq[String] = {
    keyColumnNames
  }

  /**
   * return key column alias, if not set explicitly, will return the key sql expression with non-alphanumeric removed
   * @param datum sample row of the dataset, not used in this case
   * @return
   */
  override def getKeyColumnAlias(datum: Option[Any]): Seq[String] = {
    keyAlias.getOrElse(fallbackKeyAlias)
  }

  override def appendKeyColumns(dataFrame: DataFrame): DataFrame = {
    appendKeyColumns(dataFrame, true)
  }

  /**
   * append key columns considering lateral view parameters
   * @param dataFrame source dataframe
   * @param evaluateLateralViewParams evaluate laterView parameters or not, in feature generation, set to true,
   *                                  in feature join, set to false. Because in feature generation, we simply treat
   *                                  lateral view as a special keyExtractor, but in feature join, the spark SWJ library
   *                                  will handle it in different way.
   * @return source dataframe with key columns appended
   */
  def appendKeyColumns(dataFrame: DataFrame, evaluateLateralViewParams: Boolean): DataFrame = {
    val withLateralViewDF = if (evaluateLateralViewParams && lateralView.isDefined) {
      val param = lateralView.get
      dataFrame.createOrReplaceTempView(DF_VIEW_NAME)
      val sql = s"""
         |SELECT * FROM $DF_VIEW_NAME
         |LATERAL VIEW ${param.lateralViewDef}
         |${param.lateralViewItemAlias}
    """.stripMargin
      ss.sql(sql)
    } else {
      dataFrame
    }
    keyColumnNames
      .zip(keyExprs)
      .foldLeft(withLateralViewDF)((baseDF, defs) => {
        baseDF.withColumn(defs._1, expr(defs._2))
      })
  }

  // override toString function so that SQL based anchor with same key expression can be grouped and
  // evaluated on the one dataframe in a sequential order,
  // this helps to reduce the number of joins
  // to the observation data
  // The default toString does not work, because toString of each object have different values
  override def toString: String = "SQLSourceKeyExtractor with keyExprs:" + keyExprs.mkString(" key:") + s", with lateralView ${lateralView.map(_.toString)}"

  private val JOIN_KEY_PREFIX = removeNonAlphaNumChars(super.toString) + "_"
  private val keyColumnNames = keyAlias.getOrElse(fallbackKeyAlias)
  private lazy val fallbackKeyAlias = keyExprs.map(removeNonAlphaNumChars(_))
  private val DF_VIEW_NAME = "fact_data"
  lazy val ss: SparkSession = SparkSession.builder().getOrCreate()
}
