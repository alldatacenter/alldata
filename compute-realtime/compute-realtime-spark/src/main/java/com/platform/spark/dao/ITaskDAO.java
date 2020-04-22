package com.platform.spark.dao;

import com.platform.spark.domain.Task;

/**
 * 任务管理DAO接口
 * @author wulinhao
 *
 */
public interface ITaskDAO {
	
	/**
	 * 根据主键查询任务
	 * @param taskid 主键
	 * @return 任务
	 */
	Task findById(Long taskid);
	
}
