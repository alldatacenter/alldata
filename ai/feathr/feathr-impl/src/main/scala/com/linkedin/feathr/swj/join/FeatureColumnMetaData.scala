package com.linkedin.feathr.swj.join

import com.linkedin.feathr.swj.aggregate.AggregationSpec
import com.linkedin.feathr.swj.{GroupBySpec, WindowSpec}
import com.linkedin.feathr.swj.WindowSpec
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.apache.spark.sql.types.DataType

/**
  * Metadata recording information needed to perform sliding window aggregation among
  * cached Rows.
  *
  * @param windowSpec [[WindowSpec]] with information about window width and delay
  * @param aggSpec [[AggregationSpec]] with information about the aggregation spec
  * @param groupSpec An optional [[GroupBySpec]] with information about the grouping spec
  * @param dataType [[DataType]] of the metric column. Each aggregation type supports different
  *                set of primitive types.
  * @param groupColDataType An optional [[DataType]] of the group column. Could be None if
  *                         groupSpec is None.
  */
class FeatureColumnMetaData(
    val featureName: String,
    val windowSpec: WindowSpec,
    val aggSpec: AggregationSpec,
    val groupSpec: Option[GroupBySpec],
    val dataType: DataType,
    val groupColDataType: Option[DataType]) {

  // Start index of a sliding window
  private[join] var startIndex: Int = 0
  // End index of a sliding window
  private[join] var endIndex: Int = 0
  // Aggregation result of a sliding window
  private[join] var aggResult: Any = null
  // Aggregation result of a sliding window involving group by
  private[join] var groupAggResult: Object2ObjectOpenHashMap[Any, Any] = new Object2ObjectOpenHashMap[Any, Any](100)
  // DataType of the aggregate result. Defaults to dataType, which is the DataType of the metric column.
  // It could be overwritten if the aggregate result DataType is different from metric column DataType.
  // This is the case for AVG aggregate type.
  private[swj] var aggDataType: DataType = dataType

  private[join] def tripMap(): Unit = {
    groupAggResult.clear()
    groupAggResult.trim(100)
  }

  private[join] def reset(): Unit = {
    startIndex = 0
    endIndex = 0
    aggResult = null
    tripMap()
  }
}
