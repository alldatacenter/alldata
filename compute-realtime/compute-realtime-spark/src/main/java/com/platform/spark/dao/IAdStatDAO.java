package com.platform.spark.dao;

import java.util.List;

import com.platform.spark.domain.AdStat;

/**
 * 广告实时统计DAO接口
 * @author wulinhao
 *
 */
public interface IAdStatDAO {

	void updateBatch(List<AdStat> adStats);
	
}
