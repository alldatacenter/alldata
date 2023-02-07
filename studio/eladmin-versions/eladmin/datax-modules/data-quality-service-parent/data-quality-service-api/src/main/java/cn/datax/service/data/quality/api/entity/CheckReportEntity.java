package cn.datax.service.data.quality.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 核查报告信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
@Accessors(chain = true)
@TableName(value = "quality_check_report", autoResultMap = true)
public class CheckReportEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 核查规则主键
     */
    private String checkRuleId;

    /**
     * 核查时间
     */
    private LocalDateTime checkDate;

    /**
     * 核查结果
     */
    private String checkResult;

    /**
     * 核查数量
     */
    private Integer checkTotalCount;

    /**
     * 不合规数量
     */
    private Integer checkErrorCount;

    /**
     * 核查批次号
     */
    private String checkBatch;

    /**
     * 规则名称
     */
    @TableField(exist = false)
    private String ruleName;

    /**
     * 规则类型
     */
    @TableField(exist = false)
    private String ruleType;

    /**
     * 数据源
     */
    @TableField(exist = false)
    private String ruleSource;

    /**
     * 数据表
     */
    @TableField(exist = false)
    private String ruleTable;

    /**
     * 核查字段
     */
    @TableField(exist = false)
    private String ruleColumn;
}
