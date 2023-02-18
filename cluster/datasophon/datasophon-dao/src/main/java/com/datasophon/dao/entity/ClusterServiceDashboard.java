package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群服务总览仪表盘
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-23 17:01:58
 */
@Data
@TableName("t_ddh_cluster_service_dashboard")
public class ClusterServiceDashboard implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主机
	 */
	@TableId
	private Integer id;
	/**
	 * 服务名称
	 */
	private String serviceName;
	/**
	 * 总览页面地址
	 */
	private String dashboardUrl;

}
