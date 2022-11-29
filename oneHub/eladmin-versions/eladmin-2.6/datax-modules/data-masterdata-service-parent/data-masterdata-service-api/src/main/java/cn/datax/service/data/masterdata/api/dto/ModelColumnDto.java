package cn.datax.service.data.masterdata.api.dto;

import cn.datax.common.validate.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * <p>
 * 主数据模型列信息表 实体DTO
 * </p>
 *
 * @author yuwei
 * @since 2020-08-26
 */
@ApiModel(value = "主数据模型列信息表Model")
@Data
public class ModelColumnDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "列名称")
    @NotBlank(message = "列名称不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String columnName;
    @ApiModelProperty(value = "列描述")
    @NotBlank(message = "列描述不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String columnComment;
    @ApiModelProperty(value = "列类型")
    @NotBlank(message = "列类型不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String columnType;
    @ApiModelProperty(value = "列长度")
    private String columnLength;
    @ApiModelProperty(value = "列小数位数")
    private String columnScale;
    @ApiModelProperty(value = "列默认值")
    private String defaultValue;
    @ApiModelProperty(value = "是否系统默认（0否，1是）")
    private String isSystem;
    @ApiModelProperty(value = "是否主键（0否，1是）")
    private String isPk;
    @ApiModelProperty(value = "是否必填（0否，1是）")
    private String isRequired;
    @ApiModelProperty(value = "是否为插入字段（0否，1是）")
    private String isInsert;
    @ApiModelProperty(value = "是否编辑字段（0否，1是）")
    private String isEdit;
    @ApiModelProperty(value = "是否详情字段（0否，1是）")
    private String isDetail;
    @ApiModelProperty(value = "是否列表字段（0否，1是）")
    private String isList;
    @ApiModelProperty(value = "是否查询字段（0否，1是）")
    private String isQuery;
    @ApiModelProperty(value = "查询方式（EQ等于、NE不等于、GT大于、GE大于等于、LT小于、LE小于等于、LIKE模糊、BETWEEN范围）")
    private String queryType;
    @ApiModelProperty(value = "显示类型（input文本框、textarea文本域、select下拉框、checkbox复选框、radio单选框、datetime日期控件）")
    private String htmlType;
    @ApiModelProperty(value = "排序")
    private Integer sort;
    @ApiModelProperty(value = "是否绑定数据标准（0否，1是）")
    private String isBindDict;
    @ApiModelProperty(value = "绑定数据标准类别")
    private String bindDictTypeId;
    @ApiModelProperty(value = "绑定数据标准字典字段（GB_CODE，GB_NAME）")
    private String bindDictColumn;
}
