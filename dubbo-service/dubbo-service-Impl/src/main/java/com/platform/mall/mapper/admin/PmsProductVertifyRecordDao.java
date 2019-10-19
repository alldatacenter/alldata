package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.PmsProductVertifyRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品审核日志自定义dao
 * Created by wulinhao on 2019/9/27.
 */
public interface PmsProductVertifyRecordDao {
    int insertList(@Param("list") List<PmsProductVertifyRecord> list);
}
