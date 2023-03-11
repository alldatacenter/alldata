package com.netease.arctic.spark.sql.utils

object RowDeltaUtils {
  final val OPERATION_COLUMN: String = "_arctic_upsert_op"
  final val DELETE_OPERATION: String = "D"
  final val UPDATE_OPERATION: String = "U"
  final val INSERT_OPERATION: String = "I"
}
