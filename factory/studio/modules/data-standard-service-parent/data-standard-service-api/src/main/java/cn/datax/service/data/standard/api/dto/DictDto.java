package cn.datax.service.data.standard.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
/**
 * <p>
 * 数据标准字典表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@ApiModel(value = "数据标准字典表Model")
@Data
public class DictDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "所属类别")
    @NotBlank(message = "所属类别不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String typeId;
    @ApiModelProperty(value = "标准编码")
    @NotBlank(message = "标准编码不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String gbCode;
    @ApiModelProperty(value = "标准名称")
    @NotBlank(message = "标准名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String gbName;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
