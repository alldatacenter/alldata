package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.OmsOrderOperateHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单操作记录自定义Dao
 * Created by wulinhao on 2019/9/12.
 */
public interface OmsOrderOperateHistoryDao {
    int insertList(@Param("list") List<OmsOrderOperateHistory> orderOperateHistoryList);
}
