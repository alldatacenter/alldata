package com.platform.website.util;

import com.platform.website.common.EventLogConstants;
import com.platform.website.util.ip.TaobaoIP;
import com.platform.website.util.ip.TaobaoIPResult;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class LoggerUtil {

  private static final Logger logger = Logger.getLogger(LoggerUtil.class);

  /**
   * 处理日志数据logText,返回处理结果Map
   */
  public static Map<String, String> handleLog(String logText) {
    Map<String, String> clientInfo = new HashMap<>();

    if (StringUtils.isNotBlank(logText)) {
      String[] splits = logText.trim().split(EventLogConstants.LOG_SEPARITOR);

      if (splits.length == 4) {
        //日志格式为 ip^A服务器时间^Ahost^A请求参数
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_IP, splits[0].trim()); //设置ip
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME, String.valueOf(
            TimeUtil.parseNginxServerTime2Long(splits[0].trim())));
        int index = splits[3].indexOf("?");

        if (index > -1) {
          String requestBody = splits[3].substring(index + 1);
          //处理请求参数
          handleRequestBody(requestBody, clientInfo);
          //处理userAgent
          handleUserAgent(clientInfo);
          //处理ip
          handleIp(clientInfo);
        } else {
          clientInfo.clear();
        }

      }
    }
    return clientInfo;
  }

  private static void handleIp(Map<String, String> clientInfo) {
    if (clientInfo.containsKey(EventLogConstants.LOG_COLUMN_NAME_IP)){
      String ip = clientInfo.get(EventLogConstants.LOG_COLUMN_NAME_IP);
      TaobaoIPResult taobaoIPResult = TaobaoIP.getResult(ip);
      if (taobaoIPResult != null){
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_COUNTRY, taobaoIPResult.getCountry());
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_PROVINCE, taobaoIPResult.getRegion());
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_CITY, taobaoIPResult.getCity());
      }
    }
  }

  /**
   * 处理userAgent
   * @param clientInfo
   */
  private static void handleUserAgent(Map<String, String> clientInfo) {
    if (clientInfo.containsKey(EventLogConstants.LOG_COLUMN_NAME_USER_AGENT)) {
      UserAgentUtil.UserAgentInfo info = UserAgentUtil
          .analyticUserAgent(clientInfo.get(EventLogConstants.LOG_COLUMN_NAME_USER_AGENT));
      if (info != null) {
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_OS_NAME, info.getOsName());
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_OS_VERSION, info.getOsVersion());
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_BROWSER_NAME, info.getBrowserName());
        clientInfo.put(EventLogConstants.LOG_COLUMN_NAME_BROWSER_VERSION, info.getBrowserVersion());
      }
    }
  }

  /**
   * 处理请求参数
   */
  private static void handleRequestBody(String requestBody, Map<String, String> clientInfo) {
    if (StringUtils.isNotBlank(requestBody)) {

      String[] requestParams = requestBody.split("&");
      for (String param : requestParams) {
        if (StringUtils.isNotBlank(param)) {
          int index = param.indexOf("=");
          if (index < 0) {
            logger.warn("无法解析参数:" + param + ",参数请求为:" + requestBody);
          }
          String key, value;

          try {
            key = URLDecoder.decode(param.substring(0, index), "utf-8");
            value = URLDecoder.decode(param.substring(index + 1), "utf-8");
          } catch (UnsupportedEncodingException e) {
            logger.warn("解码操作出现异常", e);
            continue;
          }
          if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
            clientInfo.put(key, value);
          }
        }
      }

    }
  }

}
