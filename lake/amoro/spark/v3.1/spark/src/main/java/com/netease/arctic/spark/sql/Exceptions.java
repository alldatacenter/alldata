package com.netease.arctic.spark.sql;

import org.apache.spark.sql.AnalysisException;
import scala.Option;

public class Exceptions {
  public static Exception analysisException(String message) {
    return new AnalysisException(message, Option.empty(), Option.empty(), Option.empty(), Option.empty());
  }
}
