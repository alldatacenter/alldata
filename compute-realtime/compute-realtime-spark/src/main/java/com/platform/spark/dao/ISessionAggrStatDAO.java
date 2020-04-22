package com.platform.spark.dao;

import com.platform.spark.domain.SessionAggrStat;

/**
 * session聚合统计模块DAO接口
 * @author wulinhao
 *
 */
public interface ISessionAggrStatDAO {

	/**
	 * 插入session聚合统计结果
	 * @param sessionAggrStat 
	 */
	void insert(SessionAggrStat sessionAggrStat);
	
}
