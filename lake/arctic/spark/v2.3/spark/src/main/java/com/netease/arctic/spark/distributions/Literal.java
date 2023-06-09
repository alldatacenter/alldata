package com.netease.arctic.spark.distributions;

import org.apache.spark.annotation.InterfaceStability;
import org.apache.spark.sql.types.DataType;

@InterfaceStability.Evolving
public interface Literal<T> extends Expression {
  /**
   * Returns the literal value.
   */
  T value();

  /**
   * Returns the SQL data type of the literal.
   */
  DataType dataType();
}
