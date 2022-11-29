package cn.datax.service.workflow.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@ApiModel(value = "任务执行Model")
@Data
public class TaskRequest implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "执行任务类型")
    private String action;
    @ApiModelProperty(value = "任务ID")
    private String taskId;
    @ApiModelProperty(value = "流程实例ID")
    private String processInstanceId;
    @ApiModelProperty(value = "用户ID")
    private String userId;
    @ApiModelProperty(value = "审批意见")
    private String message;
    @ApiModelProperty(value = "参数")
    private Map<String, Object> variables;
}
