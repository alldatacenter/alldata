package com.alibaba.tdata.aisp.server.common.factory;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.constant.AispResult;

/**
 * @ClassName: AispResponseUtil
 * @Author: dyj
 * @DATE: 2021-11-25
 * @Description:
 **/
public class AispResponseFactory {
    public static AispResult buildSuccessResult(){
        return new AispResult();
    };

    public static AispResult buildSuccessResult(Object o){
        AispResult result = new AispResult(o);
        return result;
    };

    public static AispResult buildResult(String taskUUID, String code, String message){
        AispResult result = new AispResult(taskUUID, code, message, null);
        return result;
    };

    public static AispResult buildResult(String taskUUID, int code, String message){
        AispResult result = new AispResult(taskUUID, String.valueOf(code), message, null);
        return result;
    };

}
