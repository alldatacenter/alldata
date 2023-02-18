package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datasophon.dao.enums.RoleType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 框架服务角色表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-18 14:38:53
 */
@TableName("t_ddh_frame_service_role")
@Data
public class FrameServiceRoleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId
	private Integer id;
	/**
	 * 服务id
	 */
	private Integer serviceId;
	/**
	 * 角色名称
	 */
	private String serviceRoleName;
	/**
	 * 角色类型 1:master2:worker3:client
	 */
	private RoleType serviceRoleType;
	/**
	 * 1  1+
	 */
	private String cardinality;

	private String serviceRoleJson;

	private String serviceRoleJsonMd5;

	private String frameCode;

	private String jmxPort;

	@TableField(exist = false)
	private List<String> hosts;

	private String logFile;

}
