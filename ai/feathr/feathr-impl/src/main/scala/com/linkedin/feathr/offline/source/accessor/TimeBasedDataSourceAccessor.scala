package com.linkedin.feathr.offline.source.accessor

import com.linkedin.feathr.offline.source.DataSource
import com.linkedin.feathr.offline.util.datetime.DateTimeInterval
import org.apache.spark.sql.DataFrame

/**
 * the abstract class represents a data source accessor to a time based source.
 * @param source the data source
 * @param sourceTimeInterval timespan of dataset
 * @param failOnMissingPartition controls the behavior when a missing partition is encountered.
 */
private[offline] abstract class TimeBasedDataSourceAccessor(source: DataSource, sourceTimeInterval: DateTimeInterval, failOnMissingPartition: Boolean)
    extends DataSourceAccessor(source) {

  /**
   * get source data as a dataframe
   *
   * @return the dataframe
   */
  override def get(): DataFrame = get(None)

  /**
   * get source data for a particular timespan (subset of the total time window defined)
   * the timespan of the result is the intersection of the timeIntervalOpt and the sourceTimeInterval.
   *
   * @param timeIntervalOpt timespan of the request source view, only support daily resolution so far
   * @return source view as dataframe defined by input time span, if timespan is unspecified, return union datasets.
   */
  def get(timeIntervalOpt: Option[DateTimeInterval]): DataFrame

  /**
   * Check whether the given interval overlaps with the time interval provided during the TimeSeriesSource creation.
   * Throw exception if no interval is provided during TimeSeriesSource creation.
   * @param interval the interval to check.
   * @return true if the input interval overlaps with the time interval provided during the TimeSeriesSource creation.
   */
  def overlapWithInterval(interval: DateTimeInterval): Boolean = {
    sourceTimeInterval.overlaps(interval)
  }
}
