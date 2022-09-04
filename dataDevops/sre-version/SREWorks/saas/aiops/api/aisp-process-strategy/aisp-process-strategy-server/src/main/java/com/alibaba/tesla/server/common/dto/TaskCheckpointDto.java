package com.alibaba.tesla.server.common.dto;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: ProcessCreateResult
 * @Author: dyj
 * @DATE: 2022-03-01
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCheckpointDto {

    private JSONObject data;

    private String status;

    private String code;

    private String message;

    private String taskUUID;
}
