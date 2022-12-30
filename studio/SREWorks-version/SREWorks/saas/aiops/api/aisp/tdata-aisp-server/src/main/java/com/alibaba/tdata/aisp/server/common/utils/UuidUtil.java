package com.alibaba.tdata.aisp.server.common.utils;

import java.util.UUID;

/**
 * @ClassName: UuidUtil
 * @Author: dyj
 * @DATE: 2021-08-11
 * @Description:
 **/
public class UuidUtil {
    public static String genUuid(){
        return UUID.randomUUID().toString().replace("-","")
            .concat(String.valueOf(System.currentTimeMillis()));
    }
}
