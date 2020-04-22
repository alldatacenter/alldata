package com.platform.spark.dao;

import java.util.List;

import com.platform.spark.domain.AreaTop3Product;

/**
 * 各区域top3热门商品DAO接口
 * @author wulinhao
 *
 */
public interface IAreaTop3ProductDAO {

	void insertBatch(List<AreaTop3Product> areaTopsProducts);
	
}
