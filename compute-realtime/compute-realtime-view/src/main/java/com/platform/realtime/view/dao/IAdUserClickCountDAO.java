package com.platform.realtime.view.dao;


import com.platform.realtime.view.module.AdUserClickCount;

import java.util.List;

/**
 * 用户广告点击量DAO接口
 * @author wulinhao
 *
 */
public interface IAdUserClickCountDAO {

	/**
	 * 批量更新用户广告点击量
	 * @param adUserClickCounts
	 */
	void updateBatch(List<AdUserClickCount> adUserClickCounts);
	
	/**
	 * 根据多个key查询用户广告点击量
	 * @param date 日期
	 * @param userid 用户id
	 * @param adid 广告id
	 * @return
	 */
	int findClickCountByMultiKey(String date, Long userid, Long adid);
	
}
