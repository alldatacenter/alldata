package org.dromara.cloudeon.entity;

import org.dromara.cloudeon.enums.CommandState;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 指令任务表
 */
@Entity
@Table(name = "ce_command_task")
@Data
public class CommandTaskEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;

    /**
     * 任务展示排序
     */
    private Integer taskShowSortNum;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务参数
     */
    private String taskParam;

    private String processorClassName;

    /**
     * 运行状态
     */
    @Enumerated(EnumType.STRING)
    private CommandState commandState;

    private String taskLogPath;


    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;


    private Integer commandTaskGroupId;

    private Integer commandId;

    private Integer serviceInstanceId;
    private String serviceInstanceName;


    /**
     * 进度
     */
    private Integer progress;


}



