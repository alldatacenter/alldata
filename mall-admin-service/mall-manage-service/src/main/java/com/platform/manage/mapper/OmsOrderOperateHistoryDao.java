package com.platform.manage.mapper;

import com.platform.mbg.model.OmsOrderOperateHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单操作记录自定义Dao
 * Created by wulinhao on 2019/9/12.
 */
public interface OmsOrderOperateHistoryDao {
    int insertList(@Param("list") List<OmsOrderOperateHistory> orderOperateHistoryList);
}
