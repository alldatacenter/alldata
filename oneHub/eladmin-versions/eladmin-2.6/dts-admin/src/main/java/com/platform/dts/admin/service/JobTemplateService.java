package com.platform.dts.admin.service;


import com.platform.dts.admin.entity.JobTemplate;
import com.platform.dts.core.biz.model.ReturnT;

import java.util.Map;

/**
 * core job action for Eladmin DTS
 *
 * @author AllDataDC 2022/11/28 15:30:33
 */
public interface JobTemplateService {

	/**
	 * page list
	 *
	 * @param start
	 * @param length
	 * @param jobGroup
	 * @param jobDesc
	 * @param executorHandler
	 * @param userId
	 * @return
	 */
	Map<String, Object> pageList(int start, int length, int jobGroup, String jobDesc, String executorHandler, int userId,Integer[] projectIds);
	/**
	 * add job
	 *
	 * @param jobTemplate
	 * @return
	 */
	ReturnT<String> add(JobTemplate jobTemplate);

	/**
	 * update job
	 *
	 * @param jobTemplate
	 * @return
	 */
	ReturnT<String> update(JobTemplate jobTemplate);

	/**
	 * remove job
	 * 	 *
	 * @param id
	 * @return
	 */
	ReturnT<String> remove(int id);
}
