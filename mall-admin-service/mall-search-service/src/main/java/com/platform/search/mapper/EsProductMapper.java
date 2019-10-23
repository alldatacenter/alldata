package com.platform.search.mapper;

import com.platform.search.domain.EsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 搜索系统中的商品管理自定义Dao
 * Created by wulinhao on 2019/9/19.
 */
@Mapper
public interface EsProductMapper {
    List<EsProduct> getAllEsProductList(@Param("id") Long id);

}
