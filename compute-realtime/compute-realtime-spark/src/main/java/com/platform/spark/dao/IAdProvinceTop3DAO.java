package com.platform.spark.dao;

import java.util.List;

import com.platform.spark.domain.AdProvinceTop3;

/**
 * 各省份top3热门广告DAO接口
 * @author wulinhao
 *
 */
public interface IAdProvinceTop3DAO {

	void updateBatch(List<AdProvinceTop3> adProvinceTop3s);
	
}
