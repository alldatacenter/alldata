package cn.datax.service.workflow.api.dto;

import cn.datax.service.workflow.api.enums.VariablesEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@ApiModel(value = "创建流程实例Model")
@Data
public class ProcessInstanceCreateRequest implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "流程定义ID")
    private String processDefinitionId;
    @ApiModelProperty(value = "提交人")
    private String submitter;
    @ApiModelProperty(value = "业务ID")
    private String businessKey;
    @ApiModelProperty(value = "业务类型")
    private String businessCode;
    @ApiModelProperty(value = "业务名称")
    private String businessName;
    @ApiModelProperty(value = "业务审核用户组")
    private String businessAuditGroup;
    @ApiModelProperty(value = "流程参数")
    private Map<String, Object> variables = new HashMap<>();

    public Map<String, Object> getVariables() {
        variables.put(VariablesEnum.submitter.toString(), submitter);
        variables.put(VariablesEnum.businessKey.toString(), businessKey);
        variables.put(VariablesEnum.businessCode.toString(), businessCode);
        variables.put(VariablesEnum.businessName.toString(), businessName);
        variables.put(VariablesEnum.businessAuditGroup.toString(), businessAuditGroup);
        return variables;
    }
}
