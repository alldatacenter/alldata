package cn.datax.service.data.market.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel(value = "字段信息Model")
@Data
public class FieldRule implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "字段名称")
    @NotBlank(message = "字段名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String fieldName;
    @ApiModelProperty(value = "脱敏类型")
    @NotNull(message = "脱敏类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String cipherType;
    @ApiModelProperty(value = "规则类型")
    @NotNull(message = "规则类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String cryptType;
}
