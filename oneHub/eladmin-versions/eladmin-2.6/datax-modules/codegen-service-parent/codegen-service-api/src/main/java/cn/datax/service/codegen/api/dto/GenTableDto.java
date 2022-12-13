package cn.datax.service.codegen.api.dto;

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
 * 代码生成信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-19
 */
@ApiModel(value = "代码生成信息表Model")
@Data
public class GenTableDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "表名称")
    @NotBlank(message = "表名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String tableName;
    @ApiModelProperty(value = "表描述")
    @NotBlank(message = "表描述不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String tableComment;
    @ApiModelProperty(value = "实体类名称")
    @NotBlank(message = "实体类名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String className;
    @ApiModelProperty(value = "生成包路径")
    @NotBlank(message = "生成包路径不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String packageName;
    @ApiModelProperty(value = "生成模块名")
    @NotBlank(message = "生成模块名不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String moduleName;
    @ApiModelProperty(value = "生成业务名")
    @NotBlank(message = "生成业务名不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String businessName;
    @ApiModelProperty(value = "生成功能名")
    @NotBlank(message = "生成功能名不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String functionName;
    @ApiModelProperty(value = "生成功能作者")
    @NotBlank(message = "生成功能作者不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String functionAuthor;
    @ApiModelProperty(value = "主键信息")
    private GenColumnDto pkColumn;
    @Valid
    @NotEmpty(message = "表字段不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    @Size(min = 1, message="表字段长度不能少于{min}位")
    private List<GenColumnDto> columns;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
