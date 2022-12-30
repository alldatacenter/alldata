package com.alibaba.tesla.action.params;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;


/**
 * @author huibang.lhb@alibaba-inc.com
 * Created by xunyan on 03/01/2019.
 */

@Data
@Component
public class SaveActionParam {
    //@NotBlank(message = "required")
    private String appCode;

    @NotBlank(message = "required")
    private String entityType;

    @NotBlank(message = "required")
    private String entityValue;

    @NotBlank(message = "required")
    private String actionType;

    @NotBlank(message = "required")
    private String elementId;

    private Long actionId;

    @NotNull(message = "required")
    private JSONObject actionMeta;

    @NotBlank(message = "required")
    private String actionName;

    @NotBlank(message = "required")
    private String actionLabel;

    @NotNull(message = "required")
    private JSONObject execData;

    private String empId;

    @NotNull(message = "required")
    private JSONObject processorInfo;

    @NotNull(message = "required")
    private Long createTime;

    private String node;

    private String status;

    private String role;


}
