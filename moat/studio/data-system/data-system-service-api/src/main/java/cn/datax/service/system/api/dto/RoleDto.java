package cn.datax.service.system.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@ApiModel(value = "角色Model")
@Data
public class RoleDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;

    @ApiModelProperty(value = "角色名称")
    @NotBlank(message = "角色名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String roleName;

    @ApiModelProperty(value = "角色编码")
    @NotBlank(message = "角色编码不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String roleCode;

    @ApiModelProperty(value = "数据范围")
    @NotNull(message = "数据范围不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String dataScope;

    @ApiModelProperty(value = "资源")
    @NotEmpty(message = "资源不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private List<String> menuList;

    @ApiModelProperty(value = "数据范围为2时自定义数据权限")
    private List<String> deptList;

    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
