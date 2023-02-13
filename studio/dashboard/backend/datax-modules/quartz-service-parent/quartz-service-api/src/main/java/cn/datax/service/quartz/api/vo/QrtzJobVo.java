package cn.datax.service.quartz.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 定时任务信息表 实体VO
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
public class QrtzJobVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private String remark;
    private String jobName;
    private String beanName;
    private String methodName;
    private String methodParams;
    private String cronExpression;
}
