package com.platform.realtime.view.module;

/**
 * 各区域top3热门商品
 * @author wulinhao
 *
 */
public class AreaTop3Product {

	private Long taskid;
	private String area;
	private String areaLevel;
	private Long productid;
	private String cityInfos;
	private Long clickCount;
	private String productName;
	private String productStatus;
	
	public Long getTaskid() {
		return taskid;
	}
	public void setTaskid(Long taskid) {
		this.taskid = taskid;
	}
	public String getArea() {
		return area;
	}
	public void setArea(String area) {
		this.area = area;
	}
	public String getAreaLevel() {
		return areaLevel;
	}
	public void setAreaLevel(String areaLevel) {
		this.areaLevel = areaLevel;
	}
	public Long getProductid() {
		return productid;
	}
	public void setProductid(Long productid) {
		this.productid = productid;
	}
	public String getCityInfos() {
		return cityInfos;
	}
	public void setCityInfos(String cityInfos) {
		this.cityInfos = cityInfos;
	}
	public Long getClickCount() {
		return clickCount;
	}
	public void setClickCount(Long clickCount) {
		this.clickCount = clickCount;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getProductStatus() {
		return productStatus;
	}
	public void setProductStatus(String productStatus) {
		this.productStatus = productStatus;
	}
	
}
