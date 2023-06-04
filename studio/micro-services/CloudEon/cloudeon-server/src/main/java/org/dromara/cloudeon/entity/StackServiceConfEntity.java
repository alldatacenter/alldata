package org.dromara.cloudeon.entity;

import org.dromara.cloudeon.enums.ConfValueType;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 框架服务配置表
 * 
 */
@Data
@Entity
@Table(name = "ce_stack_service_conf")
public class StackServiceConfEntity implements Serializable {
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


	private Integer stackId;

	/**
	 * 角色名称
	 */
	private String name;
	private String tag;

	/**
	 * 前端显示时的名字
	 */
	private String label;

	private String description;

	private String recommendExpression;


	private String confFile;

	private Boolean configurableInWizard;
	@Enumerated(EnumType.STRING)
	private ConfValueType valueType;

	private Integer min;
	private Integer max;
	/**
	 * 单位
	 */
	private String unit;
	/**
	 * 是否密码
	 */
	private boolean isPassword;
	/**
	 * 是否多值输入。像多了路径：/hdfs/path1,/hdfs/path2
	 */
	private boolean isMultiValue;
	/**
	 * 下拉框的选项值，逗号分隔
	 */
	private String options;


}
