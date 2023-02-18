package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datasophon.dao.enums.AlertLevel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群告警历史表 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-07 12:04:38
 */
@Data
@TableName("t_ddh_cluster_alert_history")
public class ClusterAlertHistory implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId
	private Integer id;
	/**
	 * 告警组
	 */
	private String alertGroupName;
	/**
	 * 告警指标
	 */
	private String alertTargetName;
	/**
	 * 告警详情
	 */
	private String alertInfo;
	/**
	 * 告警建议
	 */
	private String alertAdvice;
	/**
	 * 主机
	 */
	private String hostname;
	/**
	 * 告警级别 1：警告2：异常
	 */
	private AlertLevel alertLevel;
	/**
	 * 是否处理 1:未处理2：已处理
	 */
	private Integer isEnabled;
	/**
	 * 集群服务角色实例id
	 */
	private Integer serviceRoleInstanceId;
	/**
	 * 集群服务实例id
	 */
	private Integer serviceInstanceId;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 更新时间
	 */
	private Date updateTime;
	/**
	 * 集群id
	 */
	private Integer clusterId;

}
