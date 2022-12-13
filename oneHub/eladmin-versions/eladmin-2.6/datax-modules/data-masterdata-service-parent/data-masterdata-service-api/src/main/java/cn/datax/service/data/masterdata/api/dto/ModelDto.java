package cn.datax.service.data.masterdata.api.dto;

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
 * 主数据模型表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@ApiModel(value = "主数据模型表Model")
@Data
public class ModelDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "模型名称")
    @NotBlank(message = "模型名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String modelName;
    @ApiModelProperty(value = "逻辑表")
    @NotBlank(message = "逻辑表不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String modelLogicTable;
    @ApiModelProperty(value = "模型列信息")
    @Valid
    @NotEmpty(message = "模型列信息不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, message="模型列信息长度不能少于{min}位")
    private List<ModelColumnDto> modelColumns;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
