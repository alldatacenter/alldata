package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datasophon.dao.enums.MANAGED;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群主机表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-14 20:32:39
 */
@TableName("t_ddh_cluster_host")
@Data
public class ClusterHostEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 主机名
     */
    private String hostname;
    /**
     * IP
     */
    private String ip;
    /**
     * 机架
     */
    private String rack;
    /**
     * 核数
     */
    private Integer coreNum;
    /**
     * 总内存
     */
    private Integer totalMem;
    /**
     * 总磁盘
     */
    private Integer totalDisk;
    /**
     * 已用内存
     */
    private Integer usedMem;
    /**
     * 已用磁盘
     */
    private Integer usedDisk;
    /**
     * 平均负载
     */
    private String averageLoad;
    /**
     * 检测时间
     */
    private Date checkTime;
    /**
     * 集群id
     */
    private Integer clusterId;
    /**
     * 1:正常运行 2：存在异常 3、断线
     */
    private Integer hostState;
    /**
     * 1:受管 2：断线
     */
    private MANAGED managed;

    private String cpuArchitecture;

    private String nodeLabel;

    @TableField(exist = false)
    private Integer serviceRoleNum;

}
