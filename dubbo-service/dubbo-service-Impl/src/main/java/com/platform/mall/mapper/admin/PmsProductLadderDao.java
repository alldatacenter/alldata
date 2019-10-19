package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.PmsProductLadder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义会员阶梯价格Dao
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsProductLadderDao {
    int insertList(@Param("list") List<PmsProductLadder> productLadderList);
}
