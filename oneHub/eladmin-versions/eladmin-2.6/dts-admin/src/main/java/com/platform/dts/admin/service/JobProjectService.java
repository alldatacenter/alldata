package com.platform.dts.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.dts.admin.entity.JobProject;

/**
 * Job project
 *
 * @author AllDataDC
 * @version v2.1.2
 * @date 2022/11/24
 */
public interface JobProjectService extends IService<JobProject> {

    /**
     * project page
     * @param pageSize
     * @param pageNo
     * @param searchName
     * @return
     */

    IPage<JobProject> getProjectListPaging(Integer pageSize, Integer pageNo, String searchName);
}