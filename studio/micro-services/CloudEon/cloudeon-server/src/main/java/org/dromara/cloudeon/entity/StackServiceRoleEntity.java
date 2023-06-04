package org.dromara.cloudeon.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 框架服务角色表
 * 
 */
@Data
@Entity
@Table(name = "ce_stack_service_role")
public class StackServiceRoleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
	@GenericGenerator(name = "native", strategy = "native")
	private Integer id;
	/**
	 * 服务id
	 */
	private Integer serviceId;
	/**
	 * 角色名称
	 */
	private String name;

	/**
	 * 前端显示时的名字
	 */
	private String label;

	private String roleFullName;

	/**
	 * 角色ui页面链接模板
	 */
	private String linkExpression;
	/**
	 * 角色类型 master / slave
	 */
	private String type;


	private Integer sortNum;


	private Integer stackId;


	private String jmxPort;

	/**
	 * 角色分配建议配置
	 */
	private String assign;

	/**
	 * 角色支持的操作
	 */
	private String frontendOperations;

	private Integer minNum;
	private Integer fixedNum;
	private boolean needOdd;

	private String logFile;

}
