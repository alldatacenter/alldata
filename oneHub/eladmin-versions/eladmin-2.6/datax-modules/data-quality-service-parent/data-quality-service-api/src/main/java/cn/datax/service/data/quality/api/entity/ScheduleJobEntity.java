package cn.datax.service.data.quality.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 数据质量监控任务信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Data
@Accessors(chain = true)
@TableName("quality_schedule_job")
public class ScheduleJobEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * bean名称
     */
    private String beanName;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 方法参数
     */
    private String methodParams;

    /**
     * cron表达式
     */
    private String cronExpression;

    /**
     * 状态（1运行 0暂停）
     */
    private String status;
}
