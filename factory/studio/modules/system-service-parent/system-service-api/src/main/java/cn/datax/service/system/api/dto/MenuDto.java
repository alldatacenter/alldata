package cn.datax.service.system.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel(value = "资源Model")
@Data
public class MenuDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;

    @ApiModelProperty(value = "父资源ID")
    @NotBlank(message = "父资源ID不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String parentId;

    @ApiModelProperty(value = "资源名称")
    @NotBlank(message = "资源名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String menuName;

    @ApiModelProperty(value = "对应路由地址path")
    private String menuPath;

    @ApiModelProperty(value = "对应路由组件component")
    private String menuComponent;

    @ApiModelProperty(value = "对应路由默认跳转地址redirect")
    private String menuRedirect;

    @ApiModelProperty(value = "权限标识")
    private String menuPerms;

    @ApiModelProperty(value = "资源图标")
    private String menuIcon;

    @ApiModelProperty(value = "资源类型")
    @NotNull(message = "类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String menuType;

    @ApiModelProperty(value = "资源编码")
    private String menuCode;

    @ApiModelProperty(value = "隐藏")
    @NotNull(message = "隐藏不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String menuHidden;

    @ApiModelProperty(value = "排序")
    @NotNull(message = "排序不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private Integer menuSort;

    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
