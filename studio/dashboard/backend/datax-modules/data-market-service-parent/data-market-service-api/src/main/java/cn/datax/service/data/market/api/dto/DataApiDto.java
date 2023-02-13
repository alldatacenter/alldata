package cn.datax.service.data.market.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 数据API信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@ApiModel(value = "数据API信息表Model")
@Data
public class DataApiDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "API名称")
    @NotBlank(message = "API名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String apiName;
    @ApiModelProperty(value = "API版本")
    @NotBlank(message = "API版本不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String apiVersion;
    @ApiModelProperty(value = "API路径")
    @NotBlank(message = "API路径不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String apiUrl;
    @ApiModelProperty(value = "请求方式")
    @NotBlank(message = "请求方式不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String reqMethod;
    @ApiModelProperty(value = "返回格式")
    @NotBlank(message = "返回格式不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String resType;
    @ApiModelProperty(value = "IP黑名单多个用英文,隔开")
    private String deny;
	@ApiModelProperty(value = "数据源")
	@NotBlank(message = "数据源不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
	private String sourceId;
    @ApiModelProperty(value = "限流配置")
    @Valid
    private RateLimit rateLimit;
    @ApiModelProperty(value = "执行配置")
    @Valid
    private ExecuteConfig executeConfig;
    @ApiModelProperty(value = "请求参数")
    @Valid
    @NotEmpty(message = "请求参数不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, message="请求参数长度不能少于{min}位")
    private List<ReqParam> reqParams;
    @ApiModelProperty(value = "返回参数")
    @Valid
    @NotEmpty(message = "返回字段不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, message="返回字段长度不能少于{min}位")
    private List<ResParam> resParams;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
