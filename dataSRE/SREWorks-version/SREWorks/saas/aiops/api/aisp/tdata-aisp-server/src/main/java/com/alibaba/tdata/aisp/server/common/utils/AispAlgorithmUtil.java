package com.alibaba.tdata.aisp.server.common.utils;

import java.util.Arrays;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

/**
 * @ClassName: AispAlgorithmUtil
 * @Author: dyj
 * @DATE: 2022-04-28
 * @Description:
 **/
public class AispAlgorithmUtil {
    private static final List<String> empIds = Arrays.asList("265412", "197052", "101929", "262805", "264269", "136749", "136035");

    public static void filterAlgorithmParam(JSONObject response, String empId) {
        if (!empIds.contains(empId)) {
            JSONObject data = response.getJSONObject("data");
            if (data!=null) {
                data.remove("modelParam");
            }
        }
    }
}
