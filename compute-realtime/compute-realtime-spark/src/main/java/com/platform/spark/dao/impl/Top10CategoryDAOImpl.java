package com.platform.spark.dao.impl;

import com.platform.spark.dao.ITop10CategoryDAO;
import com.platform.spark.domain.Top10Category;
import com.platform.spark.jdbc.JDBCHelper;

/**
 * top10品类DAO实现
 * @author wulinhao
 *
 */
public class Top10CategoryDAOImpl implements ITop10CategoryDAO {

	@Override
	public void insert(Top10Category category) {
		String sql = "insert into top10_category values(?,?,?,?,?)";  
		
		Object[] params = new Object[]{category.getTaskid(),
				category.getCategoryid(),
				category.getClickCount(),
				category.getOrderCount(),
				category.getPayCount()};  
		
		JDBCHelper jdbcHelper = JDBCHelper.getInstance();
		jdbcHelper.executeUpdate(sql, params);
	}

}
