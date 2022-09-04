package com.alibaba.tesla.appmanager.common.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;

import org.apache.commons.lang3.StringUtils;

/**
 * @ClassName: KanikoBuildCheckUtil
 * @Author: dyj
 * @DATE: 2021-08-25
 * @Description:
 **/
public class KanikoBuildCheckUtil {
    public static final Set<String> buildKeySet = new HashSet<>(Arrays.asList("imagePush", "dockerfileTemplate", "repo", "branch"));

    public static void buildCheck(JSONObject build){
        Set<String> keySet = build.keySet();
        if (keySet==null || keySet.size()==0){
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR, "action=buildCheck|| container build mast contain key: "+ buildKeySet.toString());
        }
        for (String s : buildKeySet) {
            if (!keySet.contains(s)){
                throw new AppException(AppErrorCode.USER_CONFIG_ERROR, "action=buildCheck|| container build mast contain key: "+ buildKeySet.toString());
            }
        }
        boolean imagePush = build.getBooleanValue("imagePush");
        if (!imagePush && StringUtils.isEmpty(build.getString("useExistImage"))){
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                "action=buildCheck|| container build mast have key:useExistImage when image push choosen false!");
        }
    }

    public static void pushCheck(JSONObject build){

    }
}
