package com.platform.admin.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用于启动任务接收的实体
 *
 * @author AllDataDC
 * @ClassName TriggerJobDto * @date 2022/12/01 16:12
 */
@Data
public class TriggerJobDto implements Serializable {

    private String executorParam;

    private int jobId;
}
