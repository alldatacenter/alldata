package cn.datax.service.quartz.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 定时任务日志信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
@Accessors(chain = true)
@TableName("qrtz_job_log")
public class QrtzJobLogEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 任务ID
     */
    private String jobId;

    /**
     * 信息记录
     */
    private String msg;

    /**
     * 状态（0不启用，1启用）
     */
    private String status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}
