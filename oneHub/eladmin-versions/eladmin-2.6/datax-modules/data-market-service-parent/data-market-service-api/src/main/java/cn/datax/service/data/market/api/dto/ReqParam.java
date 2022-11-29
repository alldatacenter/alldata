package cn.datax.service.data.market.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel(value = "请求参数信息Model")
@Data
public class ReqParam implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "参数名称")
    @NotBlank(message = "参数名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String paramName;

    @ApiModelProperty(value = "是否为空")
    @NotNull(message = "是否为空不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String nullable;

    @ApiModelProperty(value = "描述")
    @NotBlank(message = "描述不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String paramComment;

    @ApiModelProperty(value = "操作符")
    @NotNull(message = "操作符不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String whereType;

    @ApiModelProperty(value = "参数类型")
    @NotNull(message = "参数类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String paramType;

    @ApiModelProperty(value = "示例值")
    @NotBlank(message = "示例值不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String exampleValue;

    @ApiModelProperty(value = "默认值")
    @NotBlank(message = "默认值不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String defaultValue;
}
