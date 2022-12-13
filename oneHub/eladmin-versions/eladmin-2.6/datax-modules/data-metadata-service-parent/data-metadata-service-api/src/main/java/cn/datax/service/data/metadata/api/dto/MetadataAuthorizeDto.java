package cn.datax.service.data.metadata.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 数据授权信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-23
 */
@ApiModel(value = "数据授权信息表Model")
@Data
public class MetadataAuthorizeDto implements Serializable {

    private static final long serialVersionUID=1L;

    @NotBlank(message = "角色ID不能为空")
    @ApiModelProperty(value = "角色ID")
    private String roleId;

    @Valid
    @ApiModelProperty(value = "授权信息")
    private List<AuthorizeData> authorizeDataList;
}
