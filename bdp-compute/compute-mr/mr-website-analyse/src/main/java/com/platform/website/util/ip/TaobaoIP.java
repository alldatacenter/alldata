package com.platform.website.util.ip;

import java.util.Map;
import org.nutz.http.Http;
import org.nutz.http.Response;
import org.nutz.json.Json;

public class TaobaoIP {
  public TaobaoIP() {
  }

  public static TaobaoIPResult getResult(String ip) {
    Response response = Http.get("http://ip.taobao.com/service/getIpInfo.php?ip=" + ip);
    TaobaoIPResult result = new TaobaoIPResult();
    if (ip != null && response.getStatus() == 200) {
      try {
        String content = response.getContent();
        Map<String, Object> contentMap = (Map)Json.fromJson(content);
        if (((Integer)((Integer)contentMap.get("code"))).intValue() == 0) {
          Map<String, Object> dataMap = (Map)contentMap.get("data");
          result.setCountry((String)dataMap.get("country"));
          result.setRegion((String)dataMap.get("region"));
          result.setCity((String)dataMap.get("city"));
          result.setCounty((String)dataMap.get("county"));
          result.setIsp((String)dataMap.get("isp"));
          result.setArea((String)dataMap.get("area"));
          result.setIp((String)dataMap.get("ip"));
          result.setCode(0);

          if (result.getCity().equals("内网IP") && result.getIsp().equals("内网IP")){
            result.setCode(0);
            result.setCountry("中国");
            result.setRegion("广东");
            result.setCity("东莞");
            result.setCounty("XX");
            result.setIsp("casc");
            result.setArea("XX");
            result.setIp(ip);
          }

          return result;
        }
      } catch (Exception var6) {
      }
    }

      result.setCode(-1);
      result.setCountry("XX");
      result.setRegion("XX");
      result.setCity("XX");
      result.setCounty("XX");
      result.setIsp("XX");
      result.setArea("XX");
      result.setIp(ip);
    return result;
  }

}
