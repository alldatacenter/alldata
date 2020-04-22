package com.platform.website.dao;

import com.platform.website.module.PaymentTypeDimension;
import org.apache.ibatis.annotations.Param;

public interface PaymentTypeDimensionMapper {

  PaymentTypeDimension getPaymentTypeDimensionById(@Param("id") int id);

  PaymentTypeDimension getPaymentTypeDimensionByType(@Param("paymentType") String paymentType);
}
