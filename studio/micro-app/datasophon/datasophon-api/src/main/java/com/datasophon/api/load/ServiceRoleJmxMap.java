package com.datasophon.api.load;

import com.datasophon.common.model.ServiceConfig;

import java.util.HashMap;
import java.util.List;

public class ServiceRoleJmxMap {
    private static HashMap<String, String> map = new HashMap<String,String>();

    public static void put(String key,String value){
        map.put(key,value);
    }

    public static String get(String key){
        return map.get(key);
    }

    public static boolean exists(String key){
        return map.containsKey(key);
    }
}
