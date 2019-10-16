package com.platform.pc.microservice.service;


import com.platform.mall.dto.EsInfo;

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
