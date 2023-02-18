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
 * @date 2022-06-14 15:50:36
 */
@Data
@TableName("t_ddh_cluster_variable")
public class ClusterVariable implements Serializable {
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
	private String variableName;
	/**
	 * 
	 */
	private String variableValue;

}
