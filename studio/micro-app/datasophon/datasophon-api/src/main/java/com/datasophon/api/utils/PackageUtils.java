package com.datasophon.api.utils;

import com.datasophon.common.Constants;
import java.util.HashMap;


public class PackageUtils {

    static HashMap<String, String> map = new HashMap<String,String>();

    public static void putServicePackageName(String frameCode, String serviceName, String dcPackageName) {
        map.put(frameCode + Constants.UNDERLINE + serviceName, dcPackageName);
    }


    public static String getServiceDcPackageName(String frameCode, String serviceName) {
        return map.get(frameCode + Constants.UNDERLINE + serviceName);
    }
}
