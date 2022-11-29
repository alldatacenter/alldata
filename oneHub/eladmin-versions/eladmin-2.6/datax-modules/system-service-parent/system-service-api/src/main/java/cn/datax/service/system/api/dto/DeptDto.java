package cn.datax.service.system.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel(value = "部门Model")
@Data
public class DeptDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;

    @ApiModelProperty(value = "父部门ID")
    @NotBlank(message = "父部门ID不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String parentId;

    @ApiModelProperty(value = "部门名称")
    @NotBlank(message = "部门名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String deptName;

    @ApiModelProperty(value = "部门编码")
    @NotBlank(message = "部门编码不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String deptNo;

    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
