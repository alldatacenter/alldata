package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.PmsSkuStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义商品sku库存Dao
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsSkuStockDao {
    /**
     * 批量插入操作
     */
    int insertList(@Param("list") List<PmsSkuStock> skuStockList);

    /**
     * 批量插入或替换操作
     */
    int replaceList(@Param("list") List<PmsSkuStock> skuStockList);
}
