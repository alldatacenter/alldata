package cn.datax.service.data.metadata.api.dto;

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
 * 数据源信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@ApiModel(value = "数据源信息表Model")
@Data
public class MetadataSourceDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "数据源类型")
    @NotBlank(message = "数据源类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String dbType;
    @ApiModelProperty(value = "数据源名称")
    @NotBlank(message = "数据源名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String sourceName;
    @ApiModelProperty(value = "数据源连接信息")
    @Valid
    private DbSchema dbSchema;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
