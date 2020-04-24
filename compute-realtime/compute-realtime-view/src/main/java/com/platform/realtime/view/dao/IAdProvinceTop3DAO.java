package com.platform.realtime.view.dao;


import com.platform.realtime.view.module.AdProvinceTop3;

import java.util.List;

/**
 * 各省份top3热门广告DAO接口
 * @author wulinhao
 *
 */
public interface IAdProvinceTop3DAO {

	void updateBatch(List<AdProvinceTop3> adProvinceTop3s);
	
}
