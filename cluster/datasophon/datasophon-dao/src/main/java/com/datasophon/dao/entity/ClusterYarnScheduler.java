package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 
 * 
 * @author dygao2
 * @email dygao2@datasophon.com
 * @date 2022-11-25 15:02:11
 */
@Data
@TableName("t_ddh_cluster_yarn_scheduler")
public class ClusterYarnScheduler implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Integer id;
	/**
	 * 
	 */
	private Integer clusterId;
	/**
	 * 
	 */
	private String scheduler;

	private Integer inUse;

}
