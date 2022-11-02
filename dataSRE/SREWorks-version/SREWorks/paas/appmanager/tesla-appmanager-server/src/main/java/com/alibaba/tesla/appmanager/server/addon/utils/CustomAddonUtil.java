package com.alibaba.tesla.appmanager.server.addon.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: CustomAddonUtil
 * @Author: dyj
 * @DATE: 2020-12-22
 * @Description:
 **/
@Slf4j
public class CustomAddonUtil {
    public static <T> T convertTo(Class<T> clazz, Object ob) {
        try {
            return JSONObject.toJavaObject((JSONObject) ob, clazz);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("actionName=convertTo|Object=%s: illegalAccessException!", ob),
                    e.getStackTrace());
        }
    }
}
