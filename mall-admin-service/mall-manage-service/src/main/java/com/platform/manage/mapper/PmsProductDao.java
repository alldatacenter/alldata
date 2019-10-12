package com.platform.manage.mapper;

import com.platform.manage.dto.PmsProductResult;
import org.apache.ibatis.annotations.Param;


/**
 * 商品自定义Dao
 * Created by wulinhao on 2019/4/26.
 */
public interface PmsProductDao {
    /**
     * 获取商品编辑信息
     */
    PmsProductResult getUpdateInfo(@Param("id") Long id);
}
