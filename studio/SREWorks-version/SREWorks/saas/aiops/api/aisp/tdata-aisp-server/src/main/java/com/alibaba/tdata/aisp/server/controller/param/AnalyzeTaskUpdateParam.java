package com.alibaba.tdata.aisp.server.controller.param;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: AnalyzeTaskUpdateParam
 * @Author: dyj
 * @DATE: 2022-03-01
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeTaskUpdateParam {
    private JSONObject data;

    private String status;

    private String code;

    private String message;

    private String taskUUID;
}
