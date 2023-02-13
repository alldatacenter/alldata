package com.alibaba.tesla.authproxy.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaUtils {

    static Logger LOG = LoggerFactory.getLogger(TeslaUtils.class);

    /**
     * 判定当前是否是主集群 (专有云环境适用，用于多集群部署场景)
     * <p>
     * 解析 DNS_PAAS_HOME_DISASTER 的实际地址，判定是否等同于 VIP_IP_PAAS_HOME_DISASTER，如果相等则为主集群，否则不是
     * <p>
     * 314版本开始根据${appmanagerUrl}/clusters返回值判断
     *
     * @return
     */
    public static boolean isPrimaryCluster() throws UnknownHostException {
        String dns = System.getenv("DNS_PAAS_HOME_DISASTER");
        String vip = System.getenv("VIP_IP_PAAS_HOME_DISASTER");
        String onPass = System.getenv("ON_PAAS");
        String cookieDomain = System.getenv("COOKIE_DOMAIN");
        String localCluster = System.getenv("ABM_CLUSTER");
        String appmanagerUrl = "http://appmanager.abm." + cookieDomain;

        if (!StringUtils.isEmpty(onPass)) {
            JSONObject teslaResult = TeslaHttpClient.getJsonWithHeader(appmanagerUrl, null, null, null, "tesla-common", null);
            JSONArray items = teslaResult.getJSONObject("data").getJSONObject("data").getJSONArray("items");
            if (items.size() == 1) {
                return true;
            }
            for (Object item : items) {
                JSONObject clusterItem = (JSONObject) item;
                String clusterId = (String) clusterItem.get("clusterId");
                Boolean masterFlag = (Boolean) clusterItem.get("masterFlag");
                if (localCluster.equals(clusterId)) {
                    return masterFlag;
                }
            }
            return false;
        }

        /*
        3.14之前版本判断逻辑
         */
        if (StringUtils.isEmpty(dns) || StringUtils.isEmpty(vip)) {
            return true;
        }

        // 相等则主集群
        String actualVip = InetAddress.getByName(dns).getHostAddress();
        return actualVip.equals(vip);
    }

    /**
     * Response写入TeslaResult，JSON
     *
     * @param response
     * @param result
     */
    public static void writeResponseJson(HttpServletResponse response, TeslaResult result) {
        PrintWriter out = null;
        try {
            String data = JSONObject.toJSONString(result);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            out = response.getWriter();
            out.append(data);
        } catch (Exception e) {
            LOG.error("Write response json failure.", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Map转String
     *
     * @param originMap
     * @return
     */
    public static String convertMap2String(Map<String, String> originMap) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> message : originMap.entrySet()) {
            result.append(message.getKey() + "," + message.getValue() + ",");
        }
        return result.toString();
    }

    /**
     * String转Map
     *
     * @param mapString
     * @return
     */
    public static Map<String, String> convertString2Map(String mapString) {
        String[] splits = mapString.split(",");
        if (splits.length < 2 || splits.length % 2 != 0) {
            return new HashMap<>();
        }
        int len = splits.length;
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < len; i = i + 2) {
            result.put(splits[i], splits[i + 1]);
        }
        return result;
    }

    /**
     * 将String类型集合数据转换为指定分割符号的字符串类型
     *
     * @param listString
     * @param s
     * @return
     */
    public static String split(List<String> listString, char s) {
        if (null == listString || listString.size() == 0) {
            return null;
        }
        StringBuffer sbuffer = new StringBuffer();
        for (int i = 0; i < listString.size(); i++) {
            if (i == listString.size() - 1) {
                sbuffer.append(listString.get(i));
            } else {
                sbuffer.append(listString.get(i) + s);
            }
        }
        return sbuffer.toString();
    }


    /**
     * 前端编辑起中输入的格式为 "谈尹|谈冬|135266",通知组件中只需要工号即可
     *
     * @param userStr
     * @return
     */
    public static String getUserEmpId(String userStr) {
        if (StringUtils.isEmpty(userStr)) {
            return "";
        }
        String[] userArray = userStr.split("\\|");
        if (null != userArray && userArray.length > 0) {
            return userArray[userArray.length - 1];
        }
        return "";
    }

    /**
     * 获取不补零的工号
     *
     * @param empId
     * @return
     */
    public static String getIntegerEmpId(String empId) {
        if (StringUtils.isEmpty(empId)) {
            return "";
        }
        Integer intEmpId = 0;
        try {
            intEmpId = Integer.valueOf(empId);
        } catch (Exception e) {
            return empId;
        }
        return String.valueOf(intEmpId);
    }

    /**
     * 前端编辑起中输入的格式为 "谈尹|谈冬|135266",通知组件中只需要工号即可
     *
     * @param value
     * @param isFull 是否获取完整工号，不足6位补零
     * @return
     */
    public static String getUserEmpId(String value, boolean isFull) {
        String retEmpId = "";
        if (StringUtils.isEmpty(value)) {
            return retEmpId;
        }
        String[] userArray = value.split("\\|");
        if (null != userArray && userArray.length > 0) {
            retEmpId = userArray[userArray.length - 1];
        }
        /**
         * 是否补充零
         */
        if (isFull) {
            StringBuffer append = new StringBuffer();
            for (int i = 0; i < 6 - retEmpId.length(); i++) {
                append.append("0");
            }
            return append.toString() + retEmpId;
        }
        return retEmpId;
    }

    /**
     * 按照指定分隔符对字符串进行分隔并获取指定索引的值
     *
     * @param value 源字符串
     * @param split 指定分隔符 如 "\\|"
     * @param index 指定数组的索引
     * @return
     */
    public static String getString(String value, String split, int index) {
        String retEmpId = "";
        if (StringUtils.isEmpty(value)) {
            return retEmpId;
        }
        String[] userArray = value.split(split);
        if (null != userArray && userArray.length > 0) {
            retEmpId = userArray[index];
        }
        return retEmpId;
    }

    /**
     * 获取本地IP地址
     *
     * @return
     */
    public static String getHostIp() {
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            return "Get host failure.";
        }
        String hostname = addr.getHostAddress();
        return hostname;
    }

    /**
     * 将MAP参数转换为&拼接的请求参数
     *
     * @param map
     * @return
     */
    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = org.apache.commons.lang.StringUtils.substringBeforeLast(s, "&");
        }
        return s;
    }
}