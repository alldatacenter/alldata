package com.platform.realtime.view.dao;


import com.platform.realtime.view.module.AdStat;

import java.util.List;

/**
 * 广告实时统计DAO接口
 * @author wlhbdp
 *
 */
public interface IAdStatDAO {

	void updateBatch(List<AdStat> adStats);
	
}
