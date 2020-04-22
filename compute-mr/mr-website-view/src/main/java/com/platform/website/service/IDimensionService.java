package com.platform.website.service;

import com.platform.website.module.BrowserDimension;
import com.platform.website.module.CurrencyTypeDimension;
import com.platform.website.module.EventDimension;
import com.platform.website.module.InboundDimension;
import com.platform.website.module.LocationDimension;
import com.platform.website.module.PaymentTypeDimension;
import com.platform.website.module.PlatformDimension;
import java.util.List;
import java.util.Map;

public interface IDimensionService {

  PlatformDimension getPlatformDimensionById(int id);

  PlatformDimension getPlatformDimensionByPlatformName(String platformName);

  Integer getKpiDimensionIdByKpiName(String kpiName);


  List<Map<String, Object>> queryDimensionData(Map<String, String> paramMap);

  BrowserDimension getBrowserDimensionById(int browserId);

  BrowserDimension getBrowserDimensionByNameAndVersion(String browser,
      String browser_version);

  LocationDimension getLocationDimensionById(int dimensionLocationId);

  LocationDimension getLocationDimensionByLocation(LocationDimension locationDimension);

  InboundDimension getInboundDimensionById(int dimensionInboundId);

  InboundDimension getInboundDimensionByName(String name);


  EventDimension getEventDimensionById(int dimensionEventId);

  EventDimension getEventDimensionByCategoryAndAction(String category, String action);

  CurrencyTypeDimension getCurrencyTypeDimensionById(int dimensionCurrencyTypeId);

  CurrencyTypeDimension getCurrencyTypeDimensionByType(String currencyType);

  PaymentTypeDimension getPaymentTypeDimensionByType(String paymentType);

  PaymentTypeDimension getPaymentTypeDimensionById(int paymentId);
}
