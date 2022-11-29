package cn.datax.service.data.market.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@ApiModel(value = "API调用参数信息Model")
@Data
public class TryParam implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "参数名称")
    @NotBlank(message = "参数名称不能为空")
    private String paramName;

    @ApiModelProperty(value = "参数值")
    private Object paramValue;
}
