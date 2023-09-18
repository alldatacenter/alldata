package datart.core.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UrlUtils {

    public static Map<String, Object> getParamsMap(String paramsStr) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(paramsStr)) {
            return map;
        }
        String[] params = paramsStr.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] param = params[i].split("=");
            if (param.length == 2) {
                map.put(param[0], param[1]);
            }
        }
        return map;
    }

    public static String covertMapToUrlParams(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        String str = sb.toString();
        if (str.endsWith("&")) {
            str = StringUtils.substringBeforeLast(str, "&");
        }
        return str;
    }
}
