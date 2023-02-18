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
 * @date 2022-08-16 16:56:00
 */
@Data
@TableName("t_ddh_cluster_service_instance_role_group")
public class ClusterServiceInstanceRoleGroup implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Integer id;
	/**
	 * 
	 */
	private String roleGroupName;
	/**
	 * 
	 */
	private Integer serviceInstanceId;
	/**
	 * 
	 */
	private String serviceName;
	/**
	 * 
	 */
	private Integer clusterId;

	private String roleGroupType;

}
