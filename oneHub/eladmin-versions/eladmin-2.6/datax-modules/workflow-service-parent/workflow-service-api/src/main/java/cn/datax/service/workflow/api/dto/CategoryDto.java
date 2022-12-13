package cn.datax.service.workflow.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>
 * 流程分类表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-10
 */
@ApiModel(value = "流程分类表Model")
@Data
public class CategoryDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "分类名称")
    private String name;
}
