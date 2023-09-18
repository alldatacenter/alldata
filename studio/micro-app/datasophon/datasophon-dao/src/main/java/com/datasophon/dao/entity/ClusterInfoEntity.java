package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datasophon.dao.enums.ClusterState;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 集群信息表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:28:12
 */
@Data
@TableName("t_ddh_cluster_info")
public class ClusterInfoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 创建人
     */
    private String createBy;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 集群名称
     */
    private String clusterName;
    /**
     * 集群编码
     */
    private String clusterCode;
    /**
     * 集群框架
     */
    private String clusterFrame;
    /**
     * 集群版本
     */
    private String frameVersion;
    /**
     * 集群状态 1:待配置2：正在运行
     */
    private ClusterState clusterState;
    /**
     * 集群框架id
     */
    private Integer frameId;

    @TableField(exist = false)
    private List<UserInfoEntity> clusterManagerList;

    @TableField(exist = false)
    private Integer clusterStateCode;

}
