package com.platform.spark.dao.impl;

import com.platform.spark.dao.ITop10SessionDAO;
import com.platform.spark.domain.Top10Session;
import com.platform.spark.jdbc.JDBCHelper;

/**
 * top10活跃session的DAO实现
 * @author wulinhao
 *
 */
public class Top10SessionDAOImpl implements ITop10SessionDAO {

	@Override
	public void insert(Top10Session top10Session) {
		String sql = "insert into top10_session values(?,?,?,?)"; 
		
		Object[] params = new Object[]{top10Session.getTaskid(),
				top10Session.getCategoryid(),
				top10Session.getSessionid(),
				top10Session.getClickCount()};
		
		JDBCHelper jdbcHelper = JDBCHelper.getInstance();
		jdbcHelper.executeUpdate(sql, params);
	}

}
