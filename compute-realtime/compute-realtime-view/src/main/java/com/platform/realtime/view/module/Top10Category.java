package com.platform.realtime.view.module;

/**
 * top10品类
 * @author wulinhao
 *
 */
public class Top10Category {

	private Long taskid;
	private Long categoryid;
	private Long clickCount;
	private Long orderCount;
	private Long payCount;
	
	public Long getTaskid() {
		return taskid;
	}
	public void setTaskid(Long taskid) {
		this.taskid = taskid;
	}
	public Long getCategoryid() {
		return categoryid;
	}
	public void setCategoryid(Long categoryid) {
		this.categoryid = categoryid;
	}
	public Long getClickCount() {
		return clickCount;
	}
	public void setClickCount(Long clickCount) {
		this.clickCount = clickCount;
	}
	public Long getOrderCount() {
		return orderCount;
	}
	public void setOrderCount(Long orderCount) {
		this.orderCount = orderCount;
	}
	public Long getPayCount() {
		return payCount;
	}
	public void setPayCount(Long payCount) {
		this.payCount = payCount;
	}
	
}
