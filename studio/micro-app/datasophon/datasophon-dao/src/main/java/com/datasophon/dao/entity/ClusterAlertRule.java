package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 规则表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-07 12:04:38
 */
@Data
@TableName("t_ddh_cluster_alert_rule")
public class ClusterAlertRule implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 自增 ID
	 */
	@TableId
	private Long id;
	/**
	 * 表达式 ID
	 */
	private Long expressionId;
	/**
	 * 是否预定义
	 */
	private String isPredefined;
	/**
	 * 比较方式 如 大于 小于 等于 等
	 */
	private String compareMethod;
	/**
	 * 阈值
	 */
	private String thresholdValue;
	/**
	 * 持续时长
	 */
	private Long persistentTime;
	/**
	 * 告警策略：单次，连续
	 */
	private String strategy;
	/**
	 * 连续告警时 间隔时长
	 */
	private Long repeatInterval;
	/**
	 * 告警级别
	 */
	private String alertLevel;
	/**
	 * 告警描述
	 */
	private String alertDesc;
	/**
	 * 接收组 ID
	 */
	private Long receiverGroupId;
	/**
	 * 状态
	 */
	private String state;
	/**
	 * 是否删除
	 */
	private String isDelete;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 修改时间
	 */
	private Date updateTime;
	/**
	 * 集群id
	 */
	private Integer clusterId;

}
