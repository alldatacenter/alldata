package com.platform.website.common;

/**
 * 统计kpi的名称枚举类
 */
public enum KpiType {

  NEW_INSTALL_USER("new_install_user"),//统计新用户的kpi
  BROWSER_NEW_INSTALL_USER("browser_new_install_user"),
  ACTIVE_USER("active_user"),
  BROWSER_ACTIVE_USER("browser_active_user"),
  ACTIVE_MEMBER("active_member"),
  BROWSER_ACTIVE_MEMBER("browser_active_member"),
  NEW_MEMBER("new_member"),
  BROWSER_NEW_MEMBER("browser_new_member"),
  INSERT_MEMBER_INFO("insert_member_info"),
  SESSIONS("sessions"),
  BROWSER_SESSIONS("browser_sessions"),
  HOURLY_ACTIVE_USER("hourly_active_user"), //按小时统计活跃用户kpi
  HOURLY_SESSIONS("hourly_sessions"),//按小时统计会话格式kpi
  HOURLY_SESSIONS_LENGTH("hourly_sessions_length"),//按小时统计会话长度kpi
  WEBSITE_PAGEVIEW("website_pageview"),//浏览器的纬度pv kpi
  LOCATION("location"),
  INBOUND("inbound"),
  INBOUND_BOUNCE("inbound_bounce"); //统计inbound维度下的跳出会话kpi

  public final String name;

  private KpiType(String name) {
    this.name = name;
  }

  public static KpiType valueOfName(String name) {
    for (KpiType type : values()) {
      if (type.name.equals(name)) {
        return type;
      }
    }
    throw new RuntimeException("指定的name不属于KpiType枚举类:" + name);
  }
}
