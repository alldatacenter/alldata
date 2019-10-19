package com.platform.mall.mapper.admin;

import com.platform.mall.dto.admin.PmsProductResult;
import org.apache.ibatis.annotations.Param;


/**
 * 商品自定义Dao
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsProductDao {
    /**
     * 获取商品编辑信息
     */
    PmsProductResult getUpdateInfo(@Param("id") Long id);
}
