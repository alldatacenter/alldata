package cn.datax.service.data.visual.api.dto;

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
 * 数据集信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@ApiModel(value = "数据集信息表Model")
@Data
public class DataSetDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "数据源")
    @NotBlank(message = "数据源不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String sourceId;
    @ApiModelProperty(value = "数据集名称")
    @NotBlank(message = "数据集名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String setName;
    @ApiModelProperty(value = "数据集sql")
    @NotBlank(message = "数据集sql不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String setSql;
    @ApiModelProperty(value = "数据模型定义")
    @Valid
    private SchemaConfig schemaConfig;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
