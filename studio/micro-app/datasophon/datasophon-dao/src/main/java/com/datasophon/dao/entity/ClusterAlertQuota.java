package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.dao.enums.QuotaState;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群告警指标表 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-24 15:10:41
 */
@Data
@TableName("t_ddh_cluster_alert_quota")
public class ClusterAlertQuota implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId
	private Integer id;
	/**
	 * 告警指标名称
	 */
	private String alertQuotaName;
	/**
	 * 服务分类
	 */
	private String serviceCategory;
	/**
	 * 告警指标表达式
	 */
	private String alertExpr;
	/**
	 * 告警级别 1:警告2：异常
	 */
	private AlertLevel alertLevel;
	/**
	 * 告警组
	 */
	private Integer alertGroupId;
	/**
	 * 通知组
	 */
	private Integer noticeGroupId;
	/**
	 * 告警建议
	 */
	private String alertAdvice;
	/**
	 * 比较方式 !=;>;<
	 */
	private String compareMethod;
	/**
	 * 告警阀值
	 */
	private Long alertThreshold;
	/**
	 * 告警策略 1:单次2：连续
	 */
	private Integer alertTactic;
	/**
	 * 间隔时长 单位分钟
	 */
	private Integer intervalDuration;
	/**
	 * 触发时长 单位秒
	 */
	private Integer triggerDuration;

	private String serviceRoleName;

	private QuotaState quotaState;

	private Date createTime;

	@TableField(exist = false)
	private Integer quotaStateCode;

	@TableField(exist = false)
	private String alertGroupName;

}
