package com.platform.spark.dao.impl;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.platform.spark.dao.IAdUserClickCountDAO;
import com.platform.spark.domain.AdUserClickCount;
import com.platform.spark.jdbc.JDBCHelper;
import com.platform.spark.model.AdUserClickCountQueryResult;

/**
 * 用户广告点击量DAO实现类
 * @author wulinhao
 *
 */
public class AdUserClickCountDAOImpl implements IAdUserClickCountDAO {

	@Override
	public void updateBatch(List<AdUserClickCount> adUserClickCounts) {
		JDBCHelper jdbcHelper = JDBCHelper.getInstance();
		
		// 首先对用户广告点击量进行分类，分成待插入的和待更新的
		List<AdUserClickCount> insertAdUserClickCounts = new ArrayList<AdUserClickCount>();
		List<AdUserClickCount> updateAdUserClickCounts = new ArrayList<AdUserClickCount>();
		
		String selectSQL = "SELECT count(*) FROM ad_user_click_count "
				+ "WHERE date=? AND user_id=? AND ad_id=? ";
		Object[] selectParams = null;
		
		for(AdUserClickCount adUserClickCount : adUserClickCounts) {
			final AdUserClickCountQueryResult queryResult = new AdUserClickCountQueryResult();
			
			selectParams = new Object[]{adUserClickCount.getDate(), 
					adUserClickCount.getUserid(), adUserClickCount.getAdid()};
			
			jdbcHelper.executeQuery(selectSQL, selectParams, new JDBCHelper.QueryCallback() {
				
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
				updateAdUserClickCounts.add(adUserClickCount);
			} else {
				insertAdUserClickCounts.add(adUserClickCount);
			}
		}
		
		// 执行批量插入
		String insertSQL = "INSERT INTO ad_user_click_count VALUES(?,?,?,?)";  
		List<Object[]> insertParamsList = new ArrayList<Object[]>();
		
		for(AdUserClickCount adUserClickCount : insertAdUserClickCounts) {
			Object[] insertParams = new Object[]{adUserClickCount.getDate(),
					adUserClickCount.getUserid(),
					adUserClickCount.getAdid(),
					adUserClickCount.getClickCount()};  
			insertParamsList.add(insertParams);
		}
		
		jdbcHelper.executeBatch(insertSQL, insertParamsList);
		
		// 执行批量更新
		
		// 突然间发现，（不好意思），小bug，其实很正常，不要太以为然
		// 作为一个课程，复杂的业务逻辑
		// 如果你就是像某些课程，来一个小案例（几个课时），不会犯什么bug
		// 但是真正课程里面讲解企业实际项目中复杂的业务需求时，都很正常，有个小bug
		
		String updateSQL = "UPDATE ad_user_click_count SET click_count=click_count+? "
				+ "WHERE date=? AND user_id=? AND ad_id=? ";  
		List<Object[]> updateParamsList = new ArrayList<Object[]>();
		
		for(AdUserClickCount adUserClickCount : updateAdUserClickCounts) {
			Object[] updateParams = new Object[]{adUserClickCount.getClickCount(),
					adUserClickCount.getDate(),
					adUserClickCount.getUserid(),
					adUserClickCount.getAdid()};  
			updateParamsList.add(updateParams);
		}
		
		jdbcHelper.executeBatch(updateSQL, updateParamsList);
	}
	
	/**
	 * 根据多个key查询用户广告点击量
	 * @param date 日期
	 * @param userid 用户id
	 * @param adid 广告id
	 * @return
	 */
	public int findClickCountByMultiKey(String date, Long userid, Long adid) {
		String sql = "SELECT click_count "
				+ "FROM ad_user_click_count "
				+ "WHERE date=? "
				+ "AND user_id=? "
				+ "AND ad_id=?";
		
		Object[] params = new Object[]{date, userid, adid};
		
		final AdUserClickCountQueryResult queryResult = new AdUserClickCountQueryResult();
		
		JDBCHelper jdbcHelper = JDBCHelper.getInstance();
		jdbcHelper.executeQuery(sql, params, new JDBCHelper.QueryCallback() {  
			
			@Override
			public void process(ResultSet rs) throws Exception {
				if(rs.next()) {
					int clickCount = rs.getInt(1);
					queryResult.setClickCount(clickCount); 
				}
			}
		});
		
		int clickCount = queryResult.getClickCount();
		
		return clickCount;
	}

}
