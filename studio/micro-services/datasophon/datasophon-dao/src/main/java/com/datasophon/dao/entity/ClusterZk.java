package com.datasophon.dao.entity;

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
 * @date 2022-09-07 10:04:16
 */
@Data
@TableName("t_ddh_cluster_zk")
public class ClusterZk implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Integer id;
	/**
	 * 
	 */
	private String zkServer;
	/**
	 * 
	 */
	private Integer myid;
	/**
	 * 
	 */
	private Integer clusterId;

}
