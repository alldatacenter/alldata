package com.platform.website.dao;

import com.platform.website.module.CurrencyTypeDimension;
import org.apache.ibatis.annotations.Param;

public interface CurrencyTypeDimensionMapper {

  CurrencyTypeDimension getCurrencyTypeDimensionById(@Param("id") int id);

  CurrencyTypeDimension getCurrencyTypeDimensionByType(@Param("currencyType") String currencyType);
}
