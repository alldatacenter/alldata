package org.dromara.cloudeon.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * 指令任务表
 */
@Entity
@Table(name = "ce_command_task_group")
@Data
public class CommandTaskGroupEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;


    private Integer CommandId;

    private Integer stepSortNum;

    private String stepName;

    private Integer serviceInstanceId;

    private String serviceInstanceName;

    private Integer stackServiceId;
    private String stackServiceName;

}



