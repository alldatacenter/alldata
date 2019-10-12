package com.platform.manage.dao;

import com.platform.manage.model.PmsProductAttributeValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品参数，商品自定义规格属性Dao
 * Created by wulinhao on 2019/4/26.
 */
public interface PmsProductAttributeValueDao {
    int insertList(@Param("list") List<PmsProductAttributeValue> productAttributeValueList);
}
