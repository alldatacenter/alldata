package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceState;
import lombok.Data;

/**
 * 集群服务表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@TableName("t_ddh_cluster_service_instance")
@Data
public class ClusterServiceInstanceEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 集群id
     */
    private Integer clusterId;
    /**
     * 服务名称
     */
    private String serviceName;

    private String label;
    /**
     * 服务状态 1、待安装 2：正在运行  3：存在告警 4:存在异常
     */
    private ServiceState serviceState;

    @TableField(exist = false)
    private Integer serviceStateCode;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建时间
     */
    private Date createTime;

    private NeedRestart needRestart;

    private Integer frameServiceId;

    @TableField(exist = false)
    private String dashboardUrl;

    @TableField(exist = false)
    private Integer alertNum;

    private Integer sortNum;
}
