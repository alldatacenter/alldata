package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.PmsMemberPrice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义会员价格Dao
 * Created by wulinhao on 2019/9/26.
 */
public interface PmsMemberPriceDao {
    int insertList(@Param("list") List<PmsMemberPrice> memberPriceList);
}
