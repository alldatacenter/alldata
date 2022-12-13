package cn.datax.service.workflow.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
/**
 * <p>
 * 业务流程配置表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-22
 */
@ApiModel(value = "业务流程配置表Model")
@Data
public class BusinessDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "业务编码")
    @NotBlank(message = "业务编码不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String businessCode;
    @ApiModelProperty(value = "业务名称")
    @NotBlank(message = "业务名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String businessName;
    @ApiModelProperty(value = "业务组件")
    @NotBlank(message = "业务组件不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String businessComponent;
    @ApiModelProperty(value = "业务审核用户组")
    @NotBlank(message = "业务审核用户组不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String businessAuditGroup;
    @ApiModelProperty(value = "流程定义ID")
    @NotBlank(message = "流程定义ID不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String processDefinitionId;
    @ApiModelProperty(value = "消息模板")
    @NotBlank(message = "消息模板不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String businessTempalte;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
