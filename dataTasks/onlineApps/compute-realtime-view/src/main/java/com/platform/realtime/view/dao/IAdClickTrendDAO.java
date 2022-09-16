package com.platform.realtime.view.dao;


import com.platform.realtime.view.module.AdClickTrend;

import java.util.List;

/**
 * 广告点击趋势DAO接口
 * @author wlhbdp
 *
 */
public interface IAdClickTrendDAO {

	void updateBatch(List<AdClickTrend> adClickTrends);
	
}
