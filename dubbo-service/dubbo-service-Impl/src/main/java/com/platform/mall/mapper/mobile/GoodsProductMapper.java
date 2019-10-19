package com.platform.mall.mapper.mobile;

import org.apache.ibatis.annotations.Param;

public interface GoodsProductMapper {
    int addStock(@Param("id") Integer id, @Param("num") Short num);
    int reduceStock(@Param("id") Integer id, @Param("num") Short num);
}
