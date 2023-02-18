package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.enums.RoleType;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群服务操作指令主机指令表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@TableName("t_ddh_cluster_service_command_host_command")
@Data
public class ClusterServiceCommandHostCommandEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String hostCommandId;
    /**
     * 指令名称
     */
    private String commandName;
    /**
     * 指令状态 1、正在运行2：成功3：失败
     */
    private CommandState commandState;

    @TableField(exist = false)
    private Integer commandStateCode;
    /**
     * 指令进度
     */
    private Integer commandProgress;
    /**
     * 主机id
     */
    private String commandHostId;

    private String commandId;

    private String hostname;
    /**
     * 服务角色名称
     */
    private String serviceRoleName;

    private RoleType serviceRoleType;

    private String resultMsg;

    private Date createTime;

    private Integer commandType;

}
