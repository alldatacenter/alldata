package cn.datax.service.data.metadata.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * <p>
 * 元数据变更记录表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-30
 */
@ApiModel(value = "元数据变更记录表Model")
@Data
public class MetadataChangeRecordDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "版本号")
    private Integer version;
    @ApiModelProperty(value = "更改类型")
    private String objectType;
    @ApiModelProperty(value = "源数据表主键")
    private String objectId;
    @ApiModelProperty(value = "修改的源数据表的字段名")
    private String fieldName;
    @ApiModelProperty(value = "该字段原来的值")
    private String fieldOldValue;
    @ApiModelProperty(value = "该字段最新的值")
    private String fieldNewValue;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
