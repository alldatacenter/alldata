package com.linkedin.feathr.offline.source.accessor

import com.linkedin.feathr.offline.config.location._
import com.linkedin.feathr.offline.source.DataSource
import com.linkedin.feathr.offline.source.dataloader.DataLoaderFactory
import com.linkedin.feathr.offline.testfwk.TestFwkUtils
import com.linkedin.feathr.offline.transformation.DataFrameExt._
import com.linkedin.feathr.offline.util.FeathrUtils
import com.linkedin.feathr.offline.util.SourceUtils.processSanityCheckMode
import org.apache.spark.sql.{DataFrame, SparkSession}
/**
 * load a dataset from a non-partitioned source.
 * @param ss the spark session
 * @param fileLoaderFactory loader for a single file
 * @param source          datasource
 * @param expectDatumType expected datum type of the loaded dataset, could be Avro GenericRecord or Avro SpecificRecord
 */
private[offline] class NonTimeBasedDataSourceAccessor(
    ss: SparkSession,
    fileLoaderFactory: DataLoaderFactory,
    source: DataSource,
    expectDatumType: Option[Class[_]])
    extends DataSourceAccessor(source) {

  /**
   * get source data as a dataframe
   *
   * @return the dataframe
   */
  override def get(): DataFrame = {
    val shouldSkipFeature = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.SKIP_MISSING_FEATURE).toBoolean
    println(s"NonTimeBasedDataSourceAccessor loading source ${source.location}")
    val df =
      try {
        source.location match {
          case SimplePath(_) => List(source.path).map(fileLoaderFactory.create(_).loadDataFrame()).reduce((x, y) => x.fuzzyUnion(y))
          case PathList(paths) => paths.map(fileLoaderFactory.create(_).loadDataFrame()).reduce((x, y) => x.fuzzyUnion(y))
          case Jdbc(_, _, _, _, _) => source.location.loadDf(SparkSession.builder().getOrCreate())
          case GenericLocation(_, _) => source.location.loadDf(SparkSession.builder().getOrCreate())
          case SparkSqlLocation(_, _) => source.location.loadDf(SparkSession.builder().getOrCreate())
          case Snowflake(_, _, _, _) => source.location.loadDf(SparkSession.builder().getOrCreate())
          case _ => fileLoaderFactory.createFromLocation(source.location).loadDataFrame()
        }
      } catch {
        case e: Exception => if (shouldSkipFeature) ss.emptyDataFrame else throw e
      }

    if (TestFwkUtils.IS_DEBUGGER_ENABLED) {
      println()
      println()
      source.pathList.foreach(sourcePath => println(f"${Console.GREEN}Source is: $sourcePath${Console.RESET}"))
      println(f"${Console.GREEN}Your source data schema is: ${Console.RESET}")
      println(f"${Console.GREEN}(meaning: |-- fieldName: type (nullable = true))${Console.RESET}")
      df.printSchema()
      println(f"${Console.GREEN}Showing source data: ${Console.RESET}")
      df.show(10)
      println()
      println()
    }
    processSanityCheckMode(ss, df)
  }
}
