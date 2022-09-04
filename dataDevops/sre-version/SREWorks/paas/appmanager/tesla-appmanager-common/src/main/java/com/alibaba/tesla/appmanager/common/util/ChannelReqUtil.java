package com.alibaba.tesla.appmanager.common.util;

/**
 * @ClassName:ChannelReqUtil
 * @author yangjie.dyj@alibaba-inc.com
 * @DATE: 2020-07-28
 * @Description:
 **/
public class ChannelReqUtil {
    public static String cleanStr(String raw){
        if(raw==null) return null;
        return raw.trim();
    }
}
