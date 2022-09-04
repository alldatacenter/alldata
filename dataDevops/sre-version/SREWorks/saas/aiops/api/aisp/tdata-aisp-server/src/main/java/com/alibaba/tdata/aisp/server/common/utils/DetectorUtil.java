package com.alibaba.tdata.aisp.server.common.utils;

import org.springframework.util.StringUtils;

/**
 * @ClassName: DetectorUtil
 * @Author: dyj
 * @DATE: 2021-12-16
 * @Description:
 **/
public class DetectorUtil {

    public static String buildUrl(String stageId, String detectorUrl) {
        if (StringUtils.isEmpty(stageId)){
            return detectorUrl;
        } else {
            return stageId.concat("-").concat(detectorUrl);
        }
    }
}
