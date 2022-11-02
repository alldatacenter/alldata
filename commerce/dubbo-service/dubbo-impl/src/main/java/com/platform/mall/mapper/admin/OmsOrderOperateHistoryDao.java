package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.OmsOrderOperateHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单操作记录自定义Dao
 * @author AllDataDC
 */
public interface OmsOrderOperateHistoryDao {
    int insertList(@Param("list") List<OmsOrderOperateHistory> orderOperateHistoryList);
}
