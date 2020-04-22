package com.platform.spark.domain;

/**
 * 页面切片转化率
 * @author wulinhao
 *
 */
public class PageSplitConvertRate {

	private Long taskid;
	private String convertRate;
	
	public Long getTaskid() {
		return taskid;
	}
	public void setTaskid(Long taskid) {
		this.taskid = taskid;
	}
	public String getConvertRate() {
		return convertRate;
	}
	public void setConvertRate(String convertRate) {
		this.convertRate = convertRate;
	}
	
}
