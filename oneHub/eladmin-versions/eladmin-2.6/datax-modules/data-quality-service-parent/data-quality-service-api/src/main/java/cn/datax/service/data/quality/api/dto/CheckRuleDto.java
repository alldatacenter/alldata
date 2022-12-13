package cn.datax.service.data.quality.api.dto;

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
 * 核查规则信息表 实体DTO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@ApiModel(value = "核查规则信息表Model")
@Data
public class CheckRuleDto implements Serializable {

    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @NotBlank(message = "主键ID不能为空", groups = {ValidationGroups.Update.class})
    private String id;
    @ApiModelProperty(value = "规则名称")
    private String ruleName;
    @ApiModelProperty(value = "规则类型")
    private String ruleTypeId;
    @ApiModelProperty(value = "核查类型")
    private String ruleItemId;
    @ApiModelProperty(value = "规则级别（3高、2中、1低）")
    private String ruleLevelId;
    @ApiModelProperty(value = "数据源类型")
    private String ruleDbType;
    @ApiModelProperty(value = "数据源主键")
    private String ruleSourceId;
    @ApiModelProperty(value = "数据源")
    private String ruleSource;
    @ApiModelProperty(value = "数据表主键")
    private String ruleTableId;
    @ApiModelProperty(value = "数据表")
    private String ruleTable;
    @ApiModelProperty(value = "数据表名称")
    private String ruleTableComment;
    @ApiModelProperty(value = "核查字段主键")
    private String ruleColumnId;
    @ApiModelProperty(value = "核查字段")
    private String ruleColumn;
    @ApiModelProperty(value = "核查字段名称")
    private String ruleColumnComment;
    @ApiModelProperty(value = "核查配置")
    @Valid
    private RuleConfig ruleConfig;
    @ApiModelProperty(value = "状态")
    @NotNull(message = "状态不能为空", groups = {ValidationGroups.Insert.class, ValidationGroups.Update.class})
    private String status;
    @ApiModelProperty(value = "备注")
    private String remark;
}
