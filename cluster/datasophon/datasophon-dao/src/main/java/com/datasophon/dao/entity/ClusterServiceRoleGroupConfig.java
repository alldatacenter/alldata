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
 * @date 2022-08-16 16:56:01
 */
@Data
@TableName("t_ddh_cluster_service_role_group_config")
public class ClusterServiceRoleGroupConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Integer id;
	/**
	 * 
	 */
	private Integer roleGroupId;
	/**
	 * 
	 */
	private String configJson;
	/**
	 * 
	 */
	private String configJsonMd5;
	/**
	 * 
	 */
	private Integer configVersion;
	/**
	 * 
	 */
	private String configFileJson;
	/**
	 * 
	 */
	private String configFileJsonMd5;
	/**
	 * 
	 */
	private Integer clusterId;
	/**
	 * 
	 */
	private Date createTime;
	/**
	 * 
	 */
	private Date updateTime;
	/**
	 * 
	 */
	private String serviceName;

}
