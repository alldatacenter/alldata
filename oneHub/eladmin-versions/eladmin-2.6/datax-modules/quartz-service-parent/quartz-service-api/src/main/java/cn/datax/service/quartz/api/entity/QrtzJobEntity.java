package cn.datax.service.quartz.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 定时任务信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("qrtz_job")
public class QrtzJobEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * Spring Bean名称
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
}
