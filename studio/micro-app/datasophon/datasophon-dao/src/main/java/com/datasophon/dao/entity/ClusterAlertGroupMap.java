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
 * @date 2022-07-13 10:01:06
 */
@Data
@TableName("t_ddh_cluster_alert_group_map")
public class ClusterAlertGroupMap implements Serializable {
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
	private Integer alertGroupId;

}
