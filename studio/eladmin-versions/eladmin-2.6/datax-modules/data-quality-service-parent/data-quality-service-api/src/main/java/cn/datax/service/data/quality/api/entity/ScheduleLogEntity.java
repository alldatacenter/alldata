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
 * 数据质量监控任务日志信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-13
 */
@Data
@Accessors(chain = true)
@TableName(value = "quality_schedule_log")
public class ScheduleLogEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 状态（1成功 0失败）
     */
    private String status;

    /**
     * 执行任务主键
     */
    private String executeJobId;

    @TableField(exist = false)
    private String executeJobName;

    /**
     * 执行规则主键
     */
    private String executeRuleId;

    @TableField(exist = false)
    private String executeRuleName;

    @TableField(exist = false)
    private String executeRuleTypeName;

    /**
     * 执行时间
     */
    private LocalDateTime executeDate;

    /**
     * 执行结果
     */
    private String executeResult;

    /**
     * 执行批次号
     */
    private String executeBatch;
}
