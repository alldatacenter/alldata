package cn.datax.service.codegen.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class GenColumnDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "列名称")
    @NotBlank(message = "列名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String columnName;
    @ApiModelProperty(value = "列描述")
    @NotBlank(message = "列描述不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String columnComment;
    @ApiModelProperty(value = "列类型")
    @NotBlank(message = "列类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String columnType;
    @ApiModelProperty(value = "JAVA类型")
    @NotBlank(message = "JAVA类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String javaType;
    @ApiModelProperty(value = "JAVA字段名")
    @NotBlank(message = "JAVA字段名不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String javaField;
    @ApiModelProperty(value = "列长度")
    private String columnLength;
    @ApiModelProperty(value = "列小数位数")
    private String columnScale;
    @ApiModelProperty(value = "是否主键（0否1是）")
    @NotNull(message = "是否主键不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String isPk;
    @ApiModelProperty(value = "是否自增（0否1是）")
    @NotNull(message = "是否自增不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String isIncrement;
    @ApiModelProperty(value = "是否必填（0否1是）")
    @NotNull(message = "是否必填不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String isRequired;
    @ApiModelProperty(value = "是否为插入字段（0否1是）")
    @NotNull(message = "是否为插入字段不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String isInsert;
    @ApiModelProperty(value = "是否编辑字段（0否1是）")
    @NotNull(message = "是否编辑字段不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String isEdit;
    @ApiModelProperty(value = "是否列表字段（0否1是）")
    @NotNull(message = "是否列表字段不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String isList;
    @ApiModelProperty(value = "是否查询字段（0否1是）")
    @NotNull(message = "是否查询字段不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String isQuery;
    @ApiModelProperty(value = "查询方式（EQ等于、NE不等于、GT大于、LT小于、LIKE模糊、BETWEEN范围）")
    @NotBlank(message = "查询方式不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String queryType;
    @ApiModelProperty(value = "显示类型（input文本框、textarea文本域、select下拉框、checkbox复选框、radio单选框、datetime日期控件）")
    @NotBlank(message = "显示类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String htmlType;
    @ApiModelProperty(value = "字典类型")
    private String dictType;
    @ApiModelProperty(value = "排序")
    private Integer sort;
}
