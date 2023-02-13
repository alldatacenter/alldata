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
 * 字典项信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @since 2023-01-17
 */
@ApiModel(value = "字典项信息表Model")
@Data
public class DictItemDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "字典ID")
    @NotBlank(message = "字典ID不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String dictId;
    @ApiModelProperty(value = "字典项文本")
    @NotBlank(message = "字典项文本不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String itemText;
    @ApiModelProperty(value = "字典项值")
    @NotBlank(message = "字典项值不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String itemValue;
    @ApiModelProperty(value = "排序")
    @NotNull(message = "排序不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private Integer itemSort;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
