package com.platform.pc.search.service;


import com.platform.pc.manager.dto.EsInfo;

/**
 * @author wulinhao
 */
public interface SearchItemService {

	/**
	 * 同步索引
	 * @return
	 */
	int importAllItems();

	/**
	 * 获取ES基本信息
	 * @return
	 */
	EsInfo getEsInfo();
}
