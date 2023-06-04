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
 * @date 2022-11-25 11:31:59
 */
@Data
@TableName("t_ddh_cluster_rack")
public class ClusterRack implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Integer id;
	/**
	 * 
	 */
	private String rack;
	/**
	 * 
	 */
	private Integer clusterId;

}
