package com.platform.realtime.view.module;

/**
 * 广告点击趋势
 * @author wulinhao
 *
 */
public class AdClickTrend {

	private String date;
	private String hour;
	private String minute;
	private Long adid;
	private Long clickCount;
	
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getHour() {
		return hour;
	}
	public void setHour(String hour) {
		this.hour = hour;
	}
	public String getMinute() {
		return minute;
	}
	public void setMinute(String minute) {
		this.minute = minute;
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
