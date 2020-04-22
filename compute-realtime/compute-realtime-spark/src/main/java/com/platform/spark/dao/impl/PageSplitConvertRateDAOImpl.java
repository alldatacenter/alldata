package com.platform.spark.dao.impl;

import com.platform.spark.dao.IPageSplitConvertRateDAO;
import com.platform.spark.domain.PageSplitConvertRate;
import com.platform.spark.jdbc.JDBCHelper;

/**
 * 页面切片转化率DAO实现类
 * @author wulinhao
 *
 */
public class PageSplitConvertRateDAOImpl implements IPageSplitConvertRateDAO {

	@Override
	public void insert(PageSplitConvertRate pageSplitConvertRate) {
		String sql = "insert into page_split_convert_rate values(?,?)";  
		Object[] params = new Object[]{pageSplitConvertRate.getTaskid(), 
				pageSplitConvertRate.getConvertRate()};
		
		JDBCHelper jdbcHelper = JDBCHelper.getInstance();
		jdbcHelper.executeUpdate(sql, params);
	}

}
