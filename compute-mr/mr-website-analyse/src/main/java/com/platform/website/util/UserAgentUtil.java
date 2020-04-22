package com.platform.website.util;

import cz.mallat.uasparser.OnlineUpdater;
import cz.mallat.uasparser.UASparser;
import java.io.IOException;
import lombok.Data;

/**
 * 解析浏览器user agent的工具类
 */
public class UserAgentUtil {

  static UASparser uaSparser = null;

  static {
    try {
      uaSparser = new UASparser(OnlineUpdater.getVendoredInputStream());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * 解析user agent字符串
   * @param userAgent
   * @return
   */
  public static UserAgentInfo analyticUserAgent(String userAgent){
    UserAgentInfo result = null;
    try {
      if (userAgent == null || userAgent.trim().isEmpty()){
        return result;
      }
      cz.mallat.uasparser.UserAgentInfo info;
      info = uaSparser.parse(userAgent);
      result = new UserAgentInfo();
      result.setBrowserName(info.getUaFamily());
      result.setBrowserVersion(info.getBrowserVersionInfo());
      result.setOsName(info.getOsFamily());
      result.setOsVersion(info.getOsName());

    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }




  @Data
  public static class UserAgentInfo {

    public String browserName;
    public String browserVersion;
    public String osName;
    public String osVersion;
  }
}
