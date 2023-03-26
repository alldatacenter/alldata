package cn.datax.service.system.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
/**
 * <p>
 * 系统参数配置信息表 实体DTO
 * </p>
 *
 * @author yuwei
 * @date 2022-05-19
 */
@ApiModel(value = "系统参数配置信息表Model")
@Data
public class ConfigDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "参数名称")
    @NotBlank(message = "参数名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String configName;
    @ApiModelProperty(value = "参数键名")
    @NotBlank(message = "参数键名不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String configKey;
    @ApiModelProperty(value = "参数键值")
    @NotBlank(message = "参数键值不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String configValue;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
