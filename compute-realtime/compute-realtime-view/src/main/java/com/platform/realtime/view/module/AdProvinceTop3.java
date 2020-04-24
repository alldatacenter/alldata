package com.platform.realtime.view.module;

/**
 * 各省top3热门广告
 * @author wulinhao
 *
 */
public class AdProvinceTop3 {

	private String date;
	private String province;
	private Long adid;
	private Long clickCount;
	
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getProvince() {
		return province;
	}
	public void setProvince(String province) {
		this.province = province;
	}
	public Long getAdid() {
		return adid;
	}
	public void setAdid(Long adid) {
		this.adid = adid;
	}
	public Long getClickCount() {
		return clickCount;
	}
	public void setClickCount(Long clickCount) {
		this.clickCount = clickCount;
	}
	
}
