package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群服务角色对应web ui表 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-30 09:35:40
 */
@Data
@TableName("t_ddh_cluster_service_role_instance_webuis")
public class ClusterServiceRoleInstanceWebuis implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId
	private Integer id;
	/**
	 * 服务角色id
	 */
	private Integer serviceRoleInstanceId;
	/**
	 * URL地址
	 */
	private String webUrl;

	private Integer serviceInstanceId;

	private String name;

}
