package cn.datax.service.data.market.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * <p>
 * 服务集成表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@ApiModel(value = "服务集成表Model")
@Data
public class ServiceIntegrationDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "服务名称")
    @NotBlank(message = "服务名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String serviceName;
    @ApiModelProperty(value = "服务类型（1http接口，2webservice接口）")
    private String serviceType;
    @ApiModelProperty(value = "http接口")
    @Valid
    private HttpService httpService;
    @ApiModelProperty(value = "webservice接口")
    @Valid
    private WebService webService;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
