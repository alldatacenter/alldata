package cn.datax.service.data.metadata.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@ApiModel(value = "数据授权信息Model")
@Data
public class AuthorizeData implements Serializable {

    private static final long serialVersionUID=1L;

    @NotBlank(message = "目标表主键ID不能为空")
    @ApiModelProperty(value = "目标表主键ID")
    private String objectId;
    @NotBlank(message = "角色ID不能为空")
    @ApiModelProperty(value = "角色ID")
    private String roleId;
    @NotBlank(message = "目标表类型不能为空")
    @ApiModelProperty(value = "目标表类型")
    private String objectType;
}
