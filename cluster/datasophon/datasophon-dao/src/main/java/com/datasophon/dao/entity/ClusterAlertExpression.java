package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 表达式常量表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-07 12:04:38
 */
@Data
@TableName("t_ddh_cluster_alert_expression")
public class ClusterAlertExpression implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 自增 ID
	 */
	@TableId
	private Long id;
	/**
	 * 指标名称
	 */
	private String name;
	/**
	 * 监控指标表达式
	 */
	private String expr;
	/**
	 * 服务类别
	 */
	private String serviceCategory;
	/**
	 * 阈值类型  BOOL  INT  FLOAT  
	 */
	private String valueType;
	/**
	 * 是否预定义
	 */
	private String isPredefined;
	/**
	 * 表达式状态
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

}
