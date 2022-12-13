package cn.datax.service.data.quality.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import cn.datax.service.data.quality.api.dto.RuleConfig;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 核查规则信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "quality_check_rule", autoResultMap = true)
public class CheckRuleEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型主键
     */
    private String ruleTypeId;

    /**
     * 核查类型主键
     */
    private String ruleItemId;

    /**
     * 规则类型
     */
    @TableField(exist = false)
    private String ruleType;

    /**
     * 规则级别（3高、2中、1低）
     */
    private String ruleLevelId;

    @TableField(exist = false)
    private String ruleLevel;

    /**
     * 数据源类型
     */
    private String ruleDbType;

    /**
     * 数据源主键
     */
    private String ruleSourceId;

    /**
     * 数据源
     */
    private String ruleSource;

    /**
     * 数据表主键
     */
    private String ruleTableId;

    /**
     * 数据表
     */
    private String ruleTable;

    /**
     * 数据表名称
     */
    private String ruleTableComment;

    /**
     * 核查字段主键
     */
    private String ruleColumnId;

    /**
     * 核查字段
     */
    private String ruleColumn;

    /**
     * 核查字段名称
     */
    private String ruleColumnComment;

    /**
     * 核查配置
     */
    @TableField(value = "config_json", typeHandler = JacksonTypeHandler.class)
    private RuleConfig ruleConfig;

    /**
     * 核查脚本
     */
    private String ruleSql;

    /**
     * 最近核查批次号（关联确定唯一核查报告）
     */
    private String lastCheckBatch;
}
