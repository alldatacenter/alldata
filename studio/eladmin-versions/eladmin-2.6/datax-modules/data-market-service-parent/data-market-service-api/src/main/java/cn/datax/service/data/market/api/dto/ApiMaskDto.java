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
 * 数据API脱敏信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@ApiModel(value = "数据API脱敏信息表Model")
@Data
public class ApiMaskDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "数据API")
    @NotBlank(message = "数据API不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String apiId;
    @ApiModelProperty(value = "脱敏名称")
    @NotBlank(message = "脱敏名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String maskName;
    @ApiModelProperty(value = "脱敏字段规则配置")
    @Valid
    @NotEmpty(message = "脱敏字段规则配置不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, message="脱敏字段规则配置长度不能少于{min}位")
    private List<FieldRule> rules;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
