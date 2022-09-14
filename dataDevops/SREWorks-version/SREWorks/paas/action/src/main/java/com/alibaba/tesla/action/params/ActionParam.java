package com.alibaba.tesla.action.params;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;


/**
 * @author huibang.lhb@alibaba-inc.com
 * Created by xunyan on 03/01/2019.
 */

@Data
public class ActionParam{

    private Long id;

    private String appCode;

    private String uuid;

    private String orderId;

    private Byte syncResult;

    private JSONObject execResult;

    private String entityType;

    private String entityValue;

    @NotBlank(message = "required")
    private String actionType;

    @NotNull
    private String elementId;

    private String status;

    @NotNull
    private Long actionId;

    private JSONObject actionMeta;

    @NotBlank(message = "required")
    private String actionName;

    @NotBlank(message = "required")
    private String actionLabel;

    private String empId;

    private JSONObject processorInfo;

    private String node;

    private String instanceId;

    private String instanceUrl;

    private Long createTime;

    private Long startTime;

    private Long endTime;

    private String role;

    private String OrderType;

    private JSONObject bpmsMetaData;

    private JSONObject bpmsFrontendResult;
}
