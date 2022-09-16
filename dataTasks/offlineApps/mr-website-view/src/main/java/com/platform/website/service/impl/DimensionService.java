package com.platform.website.service.impl;

import com.platform.website.dao.BrowserDimensionMapper;
import com.platform.website.dao.CurrencyTypeDimensionMapper;
import com.platform.website.dao.DateDimensionMapper;
import com.platform.website.dao.DimensionsMapper;
import com.platform.website.dao.EventDimensionMapper;
import com.platform.website.dao.InboundDimensionMapper;
import com.platform.website.dao.KpiDimensionMapper;
import com.platform.website.dao.LocationDimensionMapper;
import com.platform.website.dao.PaymentTypeDimensionMapper;
import com.platform.website.dao.PlatformDimensionMapper;
import com.platform.website.module.BrowserDimension;
import com.platform.website.module.CurrencyTypeDimension;
import com.platform.website.module.EventDimension;
import com.platform.website.module.InboundDimension;
import com.platform.website.module.KpiDimension;
import com.platform.website.module.LocationDimension;
import com.platform.website.module.PaymentTypeDimension;
import com.platform.website.module.PlatformDimension;
import com.platform.website.service.IDimensionService;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import lombok.Data;
import org.springframework.stereotype.Service;

@Data
@Service("dimensionService")
public class DimensionService implements IDimensionService {

  private static final String prefix = "AEDimension_";

  @Resource
  private DimensionsMapper dimensionsMapper;

  @Resource
  private DateDimensionMapper dateDimensionMapper;

  @Resource
  private PlatformDimensionMapper platformDimensionMapper;

  @Resource
  private KpiDimensionMapper kpiDimensionMapper;

  @Resource
  private BrowserDimensionMapper browserDimensionMapper;

  @Resource
  private LocationDimensionMapper locationDimensionMapper;

  @Resource
  private InboundDimensionMapper inboundDimensionMapper;

  @Resource
  private EventDimensionMapper eventDimensionMapper;

  @Resource
  private CurrencyTypeDimensionMapper currencyTypeDimensionMapper;

  @Resource
  private PaymentTypeDimensionMapper paymentTypeDimensionMapper;

  @Override
  public PlatformDimension getPlatformDimensionById(int dimensionId) {
    return this.platformDimensionMapper.getPlatformDimensionById(dimensionId);
  }

  @Override
  public PlatformDimension getPlatformDimensionByPlatformName(String platform) {
    System.out.println(platform);
    return this.platformDimensionMapper.getPlatformDimensionByPlatformName(platform);
  }

  @Override
  public Integer getKpiDimensionIdByKpiName(String kpiName) {
    KpiDimension dimension = this.kpiDimensionMapper.getKpiDimensionByKpiName(kpiName);
    if (dimension != null) {
      return dimension.getId();
    }
    return null;
  }


  @Override
  public List<Map<String, Object>> queryDimensionData(Map<String, String> paramMap) {
    return dimensionsMapper.queryDimensionData(paramMap);
  }

  @Override
  public BrowserDimension getBrowserDimensionById(int browserId) {
    return browserDimensionMapper.getBrowserDimensionById(browserId);
  }

  @Override
  public BrowserDimension getBrowserDimensionByNameAndVersion(String browser,
      String browserVersion) {
    return browserDimensionMapper.getBrowserDimensionByNameAndVersion(browser, browserVersion);
  }

  @Override
  public LocationDimension getLocationDimensionById(int dimensionLocationId) {
    return locationDimensionMapper.getLocationDimensionById(dimensionLocationId);
  }

  @Override
  public LocationDimension getLocationDimensionByLocation(LocationDimension locationDimension) {
    return locationDimensionMapper
        .getLocationDimensionByLocation(locationDimension);
  }


  @Override
  public InboundDimension getInboundDimensionById(int dimensionInboundId) {
    return inboundDimensionMapper.getInboundDimensionById(dimensionInboundId);
  }

  @Override
  public InboundDimension getInboundDimensionByName(String name) {
    return inboundDimensionMapper.getInboundDimensionByName(name);
  }

  @Override
  public EventDimension getEventDimensionById(int dimensionEventId) {
    return eventDimensionMapper.getEventDimensionById(dimensionEventId);
  }

  @Override
  public EventDimension getEventDimensionByCategoryAndAction(String category, String action) {
    return eventDimensionMapper.getEventDimensionByCategoryAndAction(category, action);
  }

  @Override
  public CurrencyTypeDimension getCurrencyTypeDimensionById(int dimensionCurrencyTypeId) {
    return currencyTypeDimensionMapper.getCurrencyTypeDimensionById(dimensionCurrencyTypeId);
  }

  @Override
  public CurrencyTypeDimension getCurrencyTypeDimensionByType(String currencyType) {
    return currencyTypeDimensionMapper.getCurrencyTypeDimensionByType(currencyType);
  }

  @Override
  public PaymentTypeDimension getPaymentTypeDimensionByType(String paymentType) {
    return paymentTypeDimensionMapper.getPaymentTypeDimensionByType(paymentType);
  }

  @Override
  public PaymentTypeDimension getPaymentTypeDimensionById(int paymentId) {
    return paymentTypeDimensionMapper.getPaymentTypeDimensionById(paymentId);
  }
}
