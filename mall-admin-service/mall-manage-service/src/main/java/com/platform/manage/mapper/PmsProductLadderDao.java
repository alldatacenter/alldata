package com.platform.manage.mapper;

import com.platform.mbg.model.PmsProductLadder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义会员阶梯价格Dao
 * Created by wulinhao on 2019/4/26.
 */
public interface PmsProductLadderDao {
    int insertList(@Param("list") List<PmsProductLadder> productLadderList);
}
