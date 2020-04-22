package com.platform.website.sdk;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 分析引擎sdk java服务器端数据收集
 */
public class AnalyticsEngineSDK {

  private static final Logger log = Logger.getGlobal();

  public static final String accessUrl = "http://master/BfImg.gif";
  private static final String platfromName = "java_server";
  private static final String sdkName = "sdk";
  private static final String version = "1";

  /**
   * 触发订单支付成功事件，发送事件数据到服务器
   */
  public static boolean onChargeSuccess(String orderId, String memberId) {
    try {
      if (isEmpty(orderId) || isEmpty(memberId)) {
        log.log(Level.WARNING, "订单id和会员id不能为空");
        return false;
      }

      Map<String, String> data = new HashMap<>();
      data.put("u_mid", memberId);
      data.put("oid", orderId);
      data.put("c_time", String.valueOf(System.currentTimeMillis()))
      ;
      data.put("ver", version);
      data.put("en", "e_cr");
      data.put("pl", platfromName);
      data.put("sdk", sdkName);

      String url = buildUrl(data);
      //发送url
      SendDataMonitor.addSendUrl(url);
      return true;
    } catch (Exception e) {
      log.log(Level.WARNING, "发送数据异常", e);
    }

    return false;
  }

  /**
   * 触发订单退款事件，发送事件数据到服务器
   */
  public static boolean onChargeRefund(String orderId, String memberId) {
    try {
      if (isEmpty(orderId) || isEmpty(memberId)) {
        log.log(Level.WARNING, "订单id和会员id不能为空");
        return false;
      }

      Map<String, String> data = new HashMap<>();
      data.put("u_mid", memberId);
      data.put("oid", orderId);
      data.put("c_time", String.valueOf(System.currentTimeMillis()))
      ;
      data.put("ver", version);
      data.put("en", "e_cr");
      data.put("pl", platfromName);
      data.put("sdk", sdkName);

      String url = buildUrl(data);
      //发送url
      SendDataMonitor.addSendUrl(url);
      return true;
    } catch (Exception e) {
      log.log(Level.WARNING, "发送数据异常", e);
    }

    return false;
  }

  private static boolean isEmpty(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static boolean isNotEmpty(String value) {
    return !(isEmpty(value));
  }

  private static String buildUrl(Map<String, String> data) throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    sb.append(accessUrl).append("?");
    for (Map.Entry<String, String> entry : data.entrySet()) {
      if (isNotEmpty(entry.getKey()) && isNotEmpty(entry.getValue())) {
        sb.append(entry.getKey().trim()).append("=")
            .append(URLEncoder.encode(entry.getValue().trim(), "utf-8")).append("&");

      }
    }

    return sb.substring(0, sb.length() - 1);
  }

}
