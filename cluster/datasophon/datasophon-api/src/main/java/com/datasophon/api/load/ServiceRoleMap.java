package com.datasophon.api.load;

import com.datasophon.common.model.ServiceRoleInfo;

import java.util.HashMap;

public class ServiceRoleMap {
    private static HashMap<String, ServiceRoleInfo> map = new HashMap<String,ServiceRoleInfo>();

    public static void put(String key,ServiceRoleInfo value){
        map.put(key,value);
    }

    public static ServiceRoleInfo get(String key){
        return map.get(key);
    }

    public static boolean exists(String key){
        return map.containsKey(key);
    }
}
