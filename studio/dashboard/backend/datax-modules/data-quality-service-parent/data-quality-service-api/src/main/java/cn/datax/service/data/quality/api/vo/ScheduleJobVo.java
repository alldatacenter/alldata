package cn.datax.service.data.quality.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 数据质量监控任务信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Data
public class ScheduleJobVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    private String jobName;
    private String beanName;
    private String methodName;
    private String methodParams;
    private String cronExpression;
}
