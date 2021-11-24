package com.platform.website.common;


/**
 * 定义日志收集得到的用户数据参数的name名称 已经event_logs这张hbase表的结构信息 name名称就是event_logs的列名
 */
public class EventLogConstants {

  public static enum EventEnum {
    LAUNCH(1, "lauch event", "e_l"),
    PAGEVIEW(2, "page view event", "e_pv"),
    CHARGEREQUEST(3, "charge request event", "e_crt"),
    CHARGESUCCESS(4, "charge success event", "e_cs"),
    CHARGEREFUND(5, "charge refund event", "e_cr"),
    EVENT(6, "event duration event", "e_e");
    public final int id;
    public final String name;
    public final String alias;

    EventEnum(int id, String name, String alias) {
      this.id = id;
      this.name = name;
      this.alias = alias;
    }


    public static EventEnum valueOfAlias(String alias){
      for (EventEnum eventEnum : values()){
        if (eventEnum.alias.equals(alias)){
          return eventEnum;
        }
      }
      return null;
    }
  }


  /**
   * 表名称
   */
  public static final String HBASE_NAME_EVENT_LOGS = "event_logs";

  /**
   * event_logs表的列簇名称
   */
  public static final String EVENT_LOGS_FAMILY_NAME = "info";

  //日志分隔符
  public static final String LOG_SEPARITOR = "\\^A";

  /**
   * 用户id
   */
  public static final String LOG_COLUMN_NAME_IP = "ip";

  /**
   * 服务器时间
   */
  public static final String LOG_COLUMN_NAME_SERVER_TIME = "s_time";

  /**
   * 事件名称
   */
  public static final String LOG_COLUMN_NAME_EVENT_NAME = "en";

  /**
   * 数据收集端的版本信息
   */
  public static final String LOG_COLUMN_NAME_VERSION = "ver";

  /**
   * UUID 用户唯一
   */
  public static final String LOG_COLUMN_NAME_UUID = "u_ud";
  /**
   * 会员唯一
   */
  public static final String LOG_COLUMN_NAME_MEMBER_ID = "u_mid";

  public static final String LOG_COLUMN_NAME_SESSION_ID = "u_sd";

  public static final String LOG_COLUMN_NAME_CLIENT_TIME = "c_time";

  public static final String LOG_COLUMN_NAME_LANGUAGE = "l";

  public static final String LOG_COLUMN_NAME_USER_AGENT = "b_iev";

  public static final String LOG_COLUMN_NAME_RESOLUTION = "b_rst";

  public static final String LOG_COLUMN_NAME_CURRENT_URL = "p_url";

  public static final String LOG_COLUMN_NAME_REFERER_URL = "p_ref";

  public static final String LOG_COLUMN_NAME_TITLE = "tt";

  public static final String LOG_COLUMN_NAME_ORDER_ID = "oid";

  public static final String LOG_COLUMN_NAME_ORDER_NAME = "on";

  public static final String LOG_COLUMN_NAME_ORDER_CURRENCY_AMOUNT = "cua";

  public static final String LOG_COLUMN_NAME_ORDER_CURRENCY_TYPE = "cut";

  public static final String LOG_COLUMN_NAME_ORDER_PAYMENT_TYPE = "pt";

  public static final String LOG_COLUMN_NAME_EVENT_CATEGORY = "ca";

  public static final String LOG_COLUMN_NAME_EVENT_ACTION = "ac";

  public static final String LOG_COLUMN_NAME_EVENT_KV_START = "kv_";

  public static final String LOG_COLUMN_NAME_EVENT_DURATION = "du";

  public static final String LOG_COLUMN_NAME_OS_NAME = "os";

  public static final String LOG_COLUMN_NAME_OS_VERSION = "os_v";

  public static final String LOG_COLUMN_NAME_BROWSER_NAME = "browser";

  public static final String LOG_COLUMN_NAME_BROWSER_VERSION = "browser_v";

  public static final String LOG_COLUMN_NAME_COUNTRY = "country";

  public static final String LOG_COLUMN_NAME_PROVINCE = "province";

  public static final String LOG_COLUMN_NAME_CITY = "city";

  public static final String LOG_COLUMN_NAME_PLATFORM = "pl";


}
