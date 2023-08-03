package com.linkedin.feathr.offline.util
import scala.reflect.runtime.universe.{Literal, Constant}

object DelimiterUtils {

  /**
   * Convert delimiter to an escape character (e.g. "   " -> "\t")
   */
  def escape(raw: String): String = {
    Literal(Constant(raw)).toString.replaceAll("\"", "")
  }

  /**
   * If rawCsvDelimiterOption is not properly set, defaults to "," as the delimiter else csvDelimiterOption
   */
  def checkDelimiterOption(csvDelimiterOption: String): String = {
    if (escape(csvDelimiterOption).trim.isEmpty) "," else csvDelimiterOption
  }

}
