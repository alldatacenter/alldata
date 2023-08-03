package com.linkedin.feathr.common;

import com.linkedin.feathr.common.util.MvelContextUDFs;

/**
 *
 * MVEL is an open-source expression language and runtime that makes it easy to write concise statements that operate
 * on structured data objects (such as Avro records), among other things.
 *
 * This class contains all the udfs used in Mvel for both online and offline
 */
public class AlienMvelContextUDFs {
  // The udf naming in this class should use a_b_c form. (to be consistent with existing Spark built-in UDFs).
  private AlienMvelContextUDFs() { }


  /**
   * convert input to upper case string
   * @param input input string
   * @return upper case input
   */
  @MvelContextUDFs.ExportToMvel
  public static String toUpperCaseExt(String input) {
    return input.toUpperCase();
  }
  /**
   * convert input to upper case string
   * @param input input string
   * @return upper case input
   */
  @MvelContextUDFs.ExportToMvel
  public static String toUpperCase(String input) {
    return input.toUpperCase();
  }

}

