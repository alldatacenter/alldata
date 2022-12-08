package com.platform.website.common;

public class AEConstants {

  //default value
  public static final String ALL = "all";
  public static final String DAY = "day";

  //ae controller api request params
  public static final String BUCKET = "bucket";
  public static final String METRIC = "metric";
  public static final String GROUP_BY = "group_by";
  public static final String DATE = "date";
  public static final String START_DATE = "start_date";
  public static final String END_DATE = "end_date";

  public static final String DIMENSION_PLATFORM_ID = "dimension_platform_id";
  public static final String PLATFORM = "platform";
  public static final String DIMENSION_BROWSER_ID = "dimension_browser_id";
  public static final String BROWSER = "browser";
  public final static String BROWSER_VERSION = "browser_version";
  public final static String DIMENSION_LOCATION_ID = "dimension_location_id";
  public final static String LOCATION_COUNTRY = "country";
  public final static String LOCATION_PROVINCE = "province";
  public final static String LOCATION_CITY = "city";
  public final static String DIMENSION_INBOUND_ID = "dimension_inbound_id";
  public final static String INBOUND_NAME = "inbound";
  public final static String DIMENSION_EVENT_ID = "dimension_event_id";
  public final static String EVENT_CATEGORY = "category";
  public final static String EVENT_ACTION = "action";
  public final static String DIMENSION_CURRENCY_TYPE_ID = "dimension_currency_type_id";
  public final static String CURRENCY_TYPE = "currency_type";
  public final static String DIMENSION_PAYMENT_TYPE_ID = "dimension_payment_type_id";
  public final static String PAYMENT_TYPE = "payment_type";

  //other
  public static final String SEPARTION_COMMA = ",";
  public static final String DELIMITER = ".";
  public static final String KPI_NAME = "kpiName";
  public static final String PRIFIX = "$";
  public static final String GROUP_FLAG = "group_";
  public static final String EMPTY_STR = "";
  public static final String TABLE_NAME = "table_name";
  public static final String DIMENSIONS = "dimensions";
  public static final String SELECT_COLUMNS = "select_columns";
  public static final String DIMENSION_NAME = "dimension_name";

  //mybatis column name
  public static final String TOTAL_USERS = "total_users";
  public static final String ACTIVE_USERS = "active_users";
  public static final String NEW_USERS = "new_users";
  public static final String ACTIVE_MEMBERS = "active_members";
  public static final String NEW_MEMBERS = "new_members";
  public static final String TOTAL_MEMBERS = "total_members";

  //bucket name
  public final static String BUCKET_USER_BEHAVIOR = "user_behavior";
  public final static String BUCKET_BROWSER = "browser";
  public final static String BUCKET_LOCATION = "location";
  public final static String BUCKET_INBOUND = "inbound";
  public final static String BUCKET_EVENT = "event";
  public final static String BUCKET_ORDER = "order";

  //metric name
  public static final String METRIC_NEW_USER_SPEED_RATE = "new_user_speed_rate";
  public static final String METRIC_ACTIVE_USER_SPEED_RATE = "active_user_speed_rate";
  public static final String METRIC_TOTAL_USER_SPEED_RATE = "total_user_speed_rate";
  public static final String METRIC_NEW_MEMBER_SPEED_RATE = "new_member_speed_rate";
  public static final String METRIC_ACTIVE_MEMBER_SPEED_RATE = "active_member_speed_rate";
  public static final String METRIC_TOTAL_MEMBER_SPEED_RATE = "total_member_speed_rate";

  public static final  String ACTIVE_USER_RATE = "active_user_rate";
}
