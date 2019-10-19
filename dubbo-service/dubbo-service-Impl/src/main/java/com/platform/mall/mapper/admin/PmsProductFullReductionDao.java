package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.PmsProductFullReduction;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义商品满减Dao
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsProductFullReductionDao {
    int insertList(@Param("list") List<PmsProductFullReduction> productFullReductionList);
}
