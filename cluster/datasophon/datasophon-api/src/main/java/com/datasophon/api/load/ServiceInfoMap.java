package com.datasophon.api.load;

import com.datasophon.common.model.ServiceInfo;
import com.datasophon.common.model.ServiceRoleInfo;

import java.util.HashMap;

public class ServiceInfoMap {
    private static HashMap<String, ServiceInfo> map = new HashMap<String,ServiceInfo>();

    public static void put(String key,ServiceInfo value){
        map.put(key,value);
    }

    public static ServiceInfo get(String key){
        return map.get(key);
    }

    public static boolean exists(String key){
        return map.containsKey(key);
    }
}
