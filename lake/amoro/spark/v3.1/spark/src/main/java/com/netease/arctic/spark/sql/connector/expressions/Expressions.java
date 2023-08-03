package com.netease.arctic.spark.sql.connector.expressions;

import org.apache.spark.sql.connector.expressions.FieldReference;
import org.apache.spark.sql.connector.expressions.NamedReference;
import scala.collection.JavaConverters;

import java.util.Arrays;

public class Expressions {

  public static NamedReference fieldReference(String... parts) {
    return new FieldReference(JavaConverters.asScalaBuffer(Arrays.asList(parts)).seq());
  }
}
