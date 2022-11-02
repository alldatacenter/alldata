package com.alibaba.tesla.server.common.util;

/**
 * @ClassName: UrlUtil
 * @Author: dyj
 * @DATE: 2022-02-28
 * @Description:
 **/
public class UrlUtil {

    public static String buildUrl(String url){
        if (!url.startsWith("http")){
            return  "http://".concat(url);
        }
        return url;
    }
}
