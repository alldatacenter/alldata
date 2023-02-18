package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datasophon.dao.enums.UserType;
import lombok.Data;

import java.io.Serializable;

/**
 * 集群角色用户中间表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:28:12
 */
@Data
@TableName("t_ddh_cluster_role_user")
public class ClusterRoleUserEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId
	private Integer id;
	/**
	 * 集群id
	 */
	private Integer clusterId;
	/**
	 * 角色id
	 */
	private UserType userType;
	/**
	 * 用户id
	 */
	private Integer userId;

}
