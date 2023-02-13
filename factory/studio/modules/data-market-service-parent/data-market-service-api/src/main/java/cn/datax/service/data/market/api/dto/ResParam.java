package cn.datax.service.data.market.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel(value = "返回参数信息Model")
@Data
public class ResParam implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "字段名称")
    @NotBlank(message = "字段名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String fieldName;

    @ApiModelProperty(value = "描述")
    @NotBlank(message = "描述不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String fieldComment;

    @ApiModelProperty(value = "数据类型")
    @NotNull(message = "数据类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String dataType;

    @ApiModelProperty(value = "示例值")
    @NotBlank(message = "示例值不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String exampleValue;

    @ApiModelProperty(value = "字段别名")
    private String fieldAliasName;
}
