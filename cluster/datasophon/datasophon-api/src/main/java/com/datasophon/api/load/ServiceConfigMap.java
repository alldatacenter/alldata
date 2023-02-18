package com.datasophon.api.load;

import com.datasophon.common.model.ServiceConfig;

import java.util.HashMap;
import java.util.List;

public class ServiceConfigMap  {

    private static HashMap<String,List<ServiceConfig>> map = new HashMap<String,List<ServiceConfig>>();

    public static void put(String key,List<ServiceConfig> configs){
        map.put(key,configs);
    }

    public static List<ServiceConfig> get(String key){
        return map.get(key);
    }

    public static boolean exists(String key){
        return map.containsKey(key);
    }
}
