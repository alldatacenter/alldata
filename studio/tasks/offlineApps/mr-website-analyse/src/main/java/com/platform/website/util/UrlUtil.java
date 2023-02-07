package com.platform.website.util;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtil {

  /**
   * 判断指定的host是否是一个有效的外链host
   */
  public static boolean isValidateInboundHost(String host) {
    if ("www.beifeng.com".equals(host) || "www.ibeifeng.com".equals(host)) {
      return false;
    }
    return true;
  }

  /**
   * 返回指定url字符串中的host
   */
  public static String getHost(String url) throws MalformedURLException {
    URL u = getURL(url);
    return u.getHost();

  }

  /**
   * 根据字符串url构建URL对象
   */
  public static URL getURL(String url) throws MalformedURLException {
    url = url.trim();
    if (!(url.startsWith("http:") || url.startsWith("https:"))) {
      url = "http://" + url;
    }
    return new URL(url);
  }

}
