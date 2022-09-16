package com.platform.website.common;

public class GlobalConstants {

  /**
   * 一天毫秒数
   */
  public final static int DAY_OF_MILLISECONDS = 86400000;

  //定义运行时间的变量名
  public final static String RUNNING_DATE_PARAMS = "RUNNING_DATE";

  public final static String DEFAULT_VALUE = "unknown";

  //纬度信息表中指定全部列
  public final static String VALUE_OF_ALL = "all";


  public final  static  String OUTPUT_COLLECTOR_KEY_PREFIX = "collector_";
  /**
   * 批量执行的key
   */
  public final  static  String JDBC_BATCH_NUMBER = "mysql.batch.number";
  /**
   * 默认批量大小
   */
  public final  static  String DEFAULT_JDBC_BATCH_NUMBER = /*"500"*/ "1";

  /**
   * 指定连接表配置为website
   */
  public final  static  String WAREHOUSE_OF_WEBSITE = "website";
  public final  static  String JDBC_DRIVE = "mysql.%s.driver";
  public final  static  String JDBC_URL = "mysql.%s.url";
  public final  static  String JDBC_USERNAME = "mysql.%s.username";
  public final  static  String JDBC_PASSWORD = "mysql.%s.password";

}
