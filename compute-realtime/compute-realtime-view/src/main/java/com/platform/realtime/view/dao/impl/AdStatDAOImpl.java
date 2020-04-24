package com.platform.realtime.view.dao.impl;


import com.platform.realtime.view.dao.IAdStatDAO;
import com.platform.realtime.view.jdbc.JDBCHelper;
import com.platform.realtime.view.module.AdStat;
import com.platform.realtime.view.module.AdStatQueryResult;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 广告实时统计DAO实现类
 * @author wulinhao
 *
 */
public class AdStatDAOImpl implements IAdStatDAO {

	@Override
	public void updateBatch(List<AdStat> adStats) {
		JDBCHelper jdbcHelper = JDBCHelper.getInstance();
		
		// 区分开来哪些是要插入的，哪些是要更新的
		List<AdStat> insertAdStats = new ArrayList<AdStat>();
		List<AdStat> updateAdStats = new ArrayList<AdStat>();
		
		String selectSQL = "SELECT count(*) "
				+ "FROM ad_stat "
				+ "WHERE date=? "
				+ "AND province=? "
				+ "AND city=? "
				+ "AND ad_id=?";
		
		for(AdStat adStat : adStats) {
			final AdStatQueryResult queryResult = new AdStatQueryResult();
			
			Object[] params = new Object[]{adStat.getDate(),
					adStat.getProvince(),
					adStat.getCity(),
					adStat.getAdid()};
			
			jdbcHelper.executeQuery(selectSQL, params, new JDBCHelper.QueryCallback() {
				
				@Override
				public void process(ResultSet rs) throws Exception {
					if(rs.next()) {
						int count = rs.getInt(1);
						queryResult.setCount(count); 
					}
				}
				
			});
			
			int count = queryResult.getCount();
			
			if(count > 0) {
				updateAdStats.add(adStat);
			} else {
				insertAdStats.add(adStat);
			}
		}
		
		// 对于需要插入的数据，执行批量插入操作
		String insertSQL = "INSERT INTO ad_stat VALUES(?,?,?,?,?)";  
		
		List<Object[]> insertParamsList = new ArrayList<Object[]>();
	
		for(AdStat adStat : insertAdStats) {
			Object[] params = new Object[]{adStat.getDate(),
					adStat.getProvince(),
					adStat.getCity(),
					adStat.getAdid(),
					adStat.getClickCount()};
			insertParamsList.add(params);
		}
		
		jdbcHelper.executeBatch(insertSQL, insertParamsList);
		
		// 对于需要更新的数据，执行批量更新操作
		String updateSQL = "UPDATE ad_stat SET click_count=? "
				+ "FROM ad_stat "
				+ "WHERE date=? "
				+ "AND province=? "
				+ "AND city=? "
				+ "AND ad_id=?";
		
		List<Object[]> updateParamsList = new ArrayList<Object[]>();
		
		for(AdStat adStat : updateAdStats) {
			Object[] params = new Object[]{adStat.getClickCount(),
					adStat.getDate(),
					adStat.getProvince(),
					adStat.getCity(),
					adStat.getAdid()};
			updateParamsList.add(params);
		}
		
		jdbcHelper.executeBatch(updateSQL, updateParamsList);
	}

}
