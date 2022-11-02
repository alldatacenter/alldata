package com.alibaba.tdata.aisp.server.common.utils;

/**
 * @ClassName: TaskCacheUtil
 * @Author: dyj
 * @DATE: 2022-03-09
 * @Description:
 **/
public class TaskCacheUtil {

    public static String genResultKey(String taskUUID){
        return taskUUID.concat("_result");
    }

    public static String genReqKey(String taskUUID) {
        return taskUUID.concat("_req");
    }

    public static String cleanKey(String key) {
        int i = key.indexOf("_");
        return key.substring(0, i);
    }
}
