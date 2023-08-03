package com.linkedin.feathr.offline.job

import com.linkedin.feathr.common.util.MvelContextUDFs
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.udf


private[offline] object FeathrUdfRegistry {

  private[feathr] def registerUdf(sparkSession: SparkSession): Unit = {
    // Spark or SQL already provides some built-in functions, like isnull, isnotnull, dayofweek, concat so those don't have to be
    // registered.
    sparkSession.udf.register("cast_double", udf(input => MvelContextUDFs.cast_double(input)))
    sparkSession.udf.register("cast_float", udf(input => MvelContextUDFs.cast_float(input)))
    sparkSession.udf.register("cast_int", udf(input => MvelContextUDFs.cast_int(input)))
    sparkSession.udf.register("and", udf((first, second) => MvelContextUDFs.and(first, second)))
    sparkSession.udf.register("or", udf((first, second) => MvelContextUDFs.or(first, second)))
    sparkSession.udf.register("not", udf(input => MvelContextUDFs.not(input)))
    // spark wont' allow return type of object. So we have to provide different functions for different return types.
    sparkSession.udf.register("if_else", udf((expression: Boolean, first: String, second: String) => MvelContextUDFs.if_else(expression, first, second)))
    sparkSession.udf.register("if_else", udf((expression: Boolean, first: Double, second: Double) => MvelContextUDFs.if_else(expression, first, second)))
    sparkSession.udf.register("if_else", udf((expression: Boolean, first: Integer, second: Integer) => MvelContextUDFs.if_else(expression, first, second)))
    sparkSession.udf.register("if_else", udf((expression: Boolean, first: Boolean, second: Boolean) => MvelContextUDFs.if_else(expression, first, second)))
  }
}
