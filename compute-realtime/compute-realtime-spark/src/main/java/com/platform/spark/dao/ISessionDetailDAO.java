package com.platform.spark.dao;

import java.util.List;

import com.platform.spark.domain.SessionDetail;

/**
 * Session明细DAO接口
 * @author wulinhao
 *
 */
public interface ISessionDetailDAO {

	/**
	 * 插入一条session明细数据
	 * @param sessionDetail 
	 */
	void insert(SessionDetail sessionDetail);
	
	/**
	 * 批量插入session明细数据
	 * @param sessionDetails
	 */
	void insertBatch(List<SessionDetail> sessionDetails);
	
}
