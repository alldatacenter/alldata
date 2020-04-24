package com.platform.website.module;


import com.platform.website.common.AEConstants;

/**
 * 定义我们的参数，mybatis中使用的具体参数
 * 
 * @author wulinhao
 *
 */
public class QueryColumn implements Cloneable {
    private String type = AEConstants.DAY; // 日期type类型
    private String startDate;
    private String endDate;
    private String location;
    private String os;
    private String tabType;// 导航名
    private int dimensionPlatformId;
    private int dimensionOSId;
    private int dimensionLocationId;
    private int dimensionBrowserId;
    private int dimensionDateId;
    private int dimensionInboundId;
    private int dimensionEventId;
    private int dimensionCurrencyTypeId;
    private int dimensionPaymentTypeId;

    private String osVersion;

    private int dateRange; // {1, 7, 30}
    private String browserVersion;
    private String browser;

    // location
    private String locationCountry;
    private String locationProvince;
    private String locationCity;

    // inbound
    private String inboundName;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getTabType() {
        return tabType;
    }

    public void setTabType(String tabType) {
        this.tabType = tabType;
    }

    public int getDimensionPlatformId() {
        return dimensionPlatformId;
    }

    public void setDimensionPlatformId(int dimensionPlatformId) {
        this.dimensionPlatformId = dimensionPlatformId;
    }

    public int getDimensionOSId() {
        return dimensionOSId;
    }

    public void setDimensionOSId(int dimensionOSId) {
        this.dimensionOSId = dimensionOSId;
    }

    public int getDimensionLocationId() {
        return dimensionLocationId;
    }

    public void setDimensionLocationId(int dimensionLocationId) {
        this.dimensionLocationId = dimensionLocationId;
    }

    public int getDimensionBrowserId() {
        return dimensionBrowserId;
    }

    public void setDimensionBrowserId(int dimensionBrowserId) {
        this.dimensionBrowserId = dimensionBrowserId;
    }

    public int getDimensionDateId() {
        return dimensionDateId;
    }

    public void setDimensionDateId(int dimensionDateId) {
        this.dimensionDateId = dimensionDateId;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public int getDateRange() {
        return dateRange;
    }

    public void setDateRange(int dateRange) {
        this.dateRange = dateRange;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    public String getLocationProvince() {
        return locationProvince;
    }

    public void setLocationProvince(String locationProvince) {
        this.locationProvince = locationProvince;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public void setLocationCity(String locationCity) {
        this.locationCity = locationCity;
    }

    public int getDimensionInboundId() {
        return dimensionInboundId;
    }

    public void setDimensionInboundId(int dimensionInboundId) {
        this.dimensionInboundId = dimensionInboundId;
    }

    public String getInboundName() {
        return inboundName;
    }

    public void setInboundName(String inboundName) {
        this.inboundName = inboundName;
    }

    public int getDimensionEventId() {
        return dimensionEventId;
    }

    public void setDimensionEventId(int dimensionEventId) {
        this.dimensionEventId = dimensionEventId;
    }

    public int getDimensionCurrencyTypeId() {
        return dimensionCurrencyTypeId;
    }

    public void setDimensionCurrencyTypeId(int dimensionCurrencyTypeId) {
        this.dimensionCurrencyTypeId = dimensionCurrencyTypeId;
    }

    public int getDimensionPaymentTypeId() {
        return dimensionPaymentTypeId;
    }

    public void setDimensionPaymentTypeId(int dimensionPaymentTypeId) {
        this.dimensionPaymentTypeId = dimensionPaymentTypeId;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}