package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datasophon.dao.enums.CommandState;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群服务操作指令主机表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@TableName("t_ddh_cluster_service_command_host")
@Data
public class ClusterServiceCommandHostEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String commandHostId;
    /**
     * 主机
     */
    private String hostname;
    /**
     * 命令状态 1：正在运行2：成功3：失败
     */
    private CommandState commandState;

    @TableField(exist = false)
    private Integer commandStateCode;
    /**
     * 命令进度
     */
    private Integer commandProgress;
    /**
     * 操作指令id
     */
    private String commandId;

    private Date createTime;

}
