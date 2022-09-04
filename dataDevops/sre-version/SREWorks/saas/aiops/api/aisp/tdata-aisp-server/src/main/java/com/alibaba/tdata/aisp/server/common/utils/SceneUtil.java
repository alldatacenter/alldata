package com.alibaba.tdata.aisp.server.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName: SceneUtil
 * @Author: dyj
 * @DATE: 2022-03-09
 * @Description:
 **/
public class SceneUtil {
    private static String regexString = "^[_a-zA-Z][a-zA-Z0-9_-]{0,94}[a-zA-Z0-9_]$";

    public static void regularSceneCode(String metricName){
        Matcher matcher = Pattern.compile(regexString).matcher(metricName);
        if(!matcher.find()){
            throw new IllegalArgumentException("action=regularSceneCode || sceneCode must match regex:"+ regexString);
        };
    }
}
