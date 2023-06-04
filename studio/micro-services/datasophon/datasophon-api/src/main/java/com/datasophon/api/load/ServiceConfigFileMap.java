package com.datasophon.api.load;

import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceConfigFileMap {
    private static HashMap<String,  Map<Generators, List<ServiceConfig>>> map = new HashMap<String, Map<Generators, List<ServiceConfig>>>();

    public static void put(String key, Map<Generators, List<ServiceConfig>> configs){
        map.put(key,configs);
    }

    public static  Map<Generators, List<ServiceConfig>> get(String key){
        return map.get(key);
    }

    public static boolean exists(String key){
        return map.containsKey(key);
    }
}
