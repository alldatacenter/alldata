package com.linkedin.feathr.offline.job

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.types._
import org.apache.spark.util.sketch.BloomFilter

/**
 * context to create a bloomfilter
 * @param dataType datatype of the column to create bloomfilter on
 * @param columnName column name to create the bloomfilter on
 * @param initializedFilter the created bloomfilter, intialized as all zero bits
 */
private case class BloomFilterContext(dataType: DataType, columnName: String, initializedFilter: BloomFilter)

/**
 * Stat functions for dataframe
 * @param df dataframe to calcuate stats
 */
class DataFrameStatFunctions private[offline] (df: DataFrame) {

  /**
   * Batch creating multiple bloomFilters, one for each column.
   * This is more efficient than using Spark implementation, as this only requires reading input dataset once,
   * while the Spark implementation will read input dataset once per bloomfilter
   * @param columnNames column names to create bloomfilters on, e.g when creating bloomfilter for multiple
   *                    join key combinations such as <x>, <y>, <x, y>
   * @param expectedNumItems expected number of distinct item in the dataframe regarding the join key
   * @param fpp false positive rate
   * @return sequence of created bloomfilters
   */
  def batchCreateBloomFilter(columnNames: Seq[String], expectedNumItems: Long, fpp: Double): Seq[BloomFilter] = {
    val context = columnNames.map(col => (col, BloomFilter.create(expectedNumItems, fpp)))
    batchBuildBloomFilter(context)
  }

  /**
   * build multiple bloomFilters
   * @param initializedBloomFilterWithColumns sequence of pair of column name and initialized bloomFilter
   * @return ready to use bloomFilters
   */
  private def batchBuildBloomFilter(initializedBloomFilterWithColumns: Seq[(String, BloomFilter)]): Seq[BloomFilter] = {
    val columnNames = initializedBloomFilterWithColumns.map(_._1)
    val columns = df.select(columnNames.head, columnNames.tail: _*)
    val colTypes = columns.schema.map(field => {
      val colType = field.dataType
      require(colType == StringType || colType.isInstanceOf[NumericType], s"Bloom filter only supports string type and integral types, but got $colType.")
      colType
    })
    val bloomFiltersContext = colTypes.zip(initializedBloomFilterWithColumns).map {
      case (dType, (col, filter)) =>
        BloomFilterContext(dType, col, filter)
    }

    val filterContexts =
      columns.queryExecution.toRdd
        .treeAggregate(bloomFiltersContext)(
          (accBloomFiltersContext: Seq[BloomFilterContext], row: InternalRow) => {
            accBloomFiltersContext.zipWithIndex.foreach {
              case (bloomFilterContext, ordinal) =>
                val colType = bloomFilterContext.dataType
                val updater: (BloomFilter, InternalRow) => Unit =
                  colType match {
                    // For string type, we can get bytes of our `UTF8String` directly, and call the `putBinary`
                    // instead of `putString` to avoid unnecessary conversion.
                    case StringType =>
                      (filter, row) =>
                        filter.putBinary(row.getUTF8String(ordinal).getBytes)
                    case ByteType =>
                      (filter, row) =>
                        filter.putLong(row.getByte(ordinal))
                    case ShortType =>
                      (filter, row) =>
                        filter.putLong(row.getShort(ordinal))
                    case IntegerType =>
                      (filter, row) =>
                        filter.putLong(row.getInt(ordinal))
                    case LongType =>
                      (filter, row) =>
                        filter.putLong(row.getLong(ordinal))
                    case _ =>
                      throw new IllegalArgumentException(
                        s"Bloom filter only supports string type and integral types, " +
                          s"and does not support type $colType.")
                  }
                val filter = bloomFilterContext.initializedFilter
                updater(filter, row)
            }
            accBloomFiltersContext
          },
          (filterContexts1, filterContexts2) =>
            filterContexts1.zip(filterContexts2).map {
              case (filterContext1, filterContext2) =>
                filterContext1.initializedFilter.mergeInPlace(filterContext2.initializedFilter)
                filterContext1
          })
    filterContexts.map(_.initializedFilter)
  }
}
