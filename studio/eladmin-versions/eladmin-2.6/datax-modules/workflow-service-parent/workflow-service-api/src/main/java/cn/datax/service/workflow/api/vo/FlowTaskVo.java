package cn.datax.service.workflow.api.vo;

import cn.datax.service.workflow.api.enums.VariablesEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class FlowTaskVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String name;
    private String owner;
    private String assignee;
    private Date createTime;
    private Integer suspensionState;
    private String executionId;
    private String processInstanceId;
    private String processDefinitionId;
    private String taskDefinitionKey;
    private Date claimTime;
    private String tenantId;
    /**
     * 是否委派任务
     */
    private Boolean isDelegation;
    /**
     * 业务相关
     */
    private String businessKey;
    private String businessCode;
    private String businessName;
    private Map<String,Object> variables;

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
        if(null != variables){
            //放入业务常量
            this.businessKey = (String) variables.get(VariablesEnum.businessKey.toString());
            this.businessCode = (String) variables.get(VariablesEnum.businessCode.toString());
            this.businessName = (String) variables.get(VariablesEnum.businessName.toString());
        }
    }
}
