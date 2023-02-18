package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-07-13 19:34:14
 */
@Data
@TableName("t_ddh_cluster_yarn_queue")
public class ClusterYarnQueue implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Integer id;
	/**
	 * 
	 */
	private String queueName;
	/**
	 * 
	 */
	private Integer minCore;
	/**
	 * 
	 */
	private Integer minMem;
	/**
	 * 
	 */
	private Integer maxCore;
	/**
	 * 
	 */
	private Integer maxMem;
	/**
	 * 
	 */
	private Integer appNum;
	/**
	 * 
	 */
	private Integer weight;
	/**
	 * 
	 */
	private String schedulePolicy;
	/**
	 * 1: true 2:false
	 */
	private Integer allowPreemption;

	private Integer clusterId;

	private Date createTime;

	private String amShare;

	@TableField(exist = false)
	private String minResources;

	@TableField(exist = false)
	private String maxResources;


}
