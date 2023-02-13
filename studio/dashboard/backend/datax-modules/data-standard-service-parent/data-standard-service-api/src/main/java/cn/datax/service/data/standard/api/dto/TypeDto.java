package cn.datax.service.data.standard.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
/**
 * <p>
 * 数据标准类别表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@ApiModel(value = "数据标准类别表Model")
@Data
public class TypeDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "标准类别编码")
    @NotBlank(message = "标准类别编码不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String gbTypeCode;
    @ApiModelProperty(value = "标准类别名称")
    @NotBlank(message = "标准类别名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String gbTypeName;
}
