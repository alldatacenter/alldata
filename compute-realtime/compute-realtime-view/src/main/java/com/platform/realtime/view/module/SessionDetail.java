package com.platform.realtime.view.module;

/**
 * Session明细
 * @author wulinhao
 *
 */
public class SessionDetail {

	private Long taskid;
	private Long userid;
	private String sessionid;
	private Long pageid;
	private String actionTime;
	private String searchKeyword;
	private Long clickCategoryId;
	private Long clickProductId;
	private String orderCategoryIds;
	private String orderProductIds;
	private String payCategoryIds;
	private String payProductIds;
	
	public Long getTaskid() {
		return taskid;
	}
	public void setTaskid(Long taskid) {
		this.taskid = taskid;
	}
	public Long getUserid() {
		return userid;
	}
	public void setUserid(Long userid) {
		this.userid = userid;
	}
	public String getSessionid() {
		return sessionid;
	}
	public void setSessionid(String sessionid) {
		this.sessionid = sessionid;
	}
	public Long getPageid() {
		return pageid;
	}
	public void setPageid(Long pageid) {
		this.pageid = pageid;
	}
	public String getActionTime() {
		return actionTime;
	}
	public void setActionTime(String actionTime) {
		this.actionTime = actionTime;
	}
	public String getSearchKeyword() {
		return searchKeyword;
	}
	public void setSearchKeyword(String searchKeyword) {
		this.searchKeyword = searchKeyword;
	}
	public Long getClickCategoryId() {
		return clickCategoryId;
	}
	public void setClickCategoryId(Long clickCategoryId) {
		this.clickCategoryId = clickCategoryId;
	}
	public Long getClickProductId() {
		return clickProductId;
	}
	public void setClickProductId(Long clickProductId) {
		this.clickProductId = clickProductId;
	}
	public String getOrderCategoryIds() {
		return orderCategoryIds;
	}
	public void setOrderCategoryIds(String orderCategoryIds) {
		this.orderCategoryIds = orderCategoryIds;
	}
	public String getOrderProductIds() {
		return orderProductIds;
	}
	public void setOrderProductIds(String orderProductIds) {
		this.orderProductIds = orderProductIds;
	}
	public String getPayCategoryIds() {
		return payCategoryIds;
	}
	public void setPayCategoryIds(String payCategoryIds) {
		this.payCategoryIds = payCategoryIds;
	}
	public String getPayProductIds() {
		return payProductIds;
	}
	public void setPayProductIds(String payProductIds) {
		this.payProductIds = payProductIds;
	}
	
}
