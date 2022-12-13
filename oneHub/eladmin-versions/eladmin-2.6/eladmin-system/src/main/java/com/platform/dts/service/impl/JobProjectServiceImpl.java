package com.platform.dts.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.dts.mapper.JobProjectMapper;
import com.platform.dts.service.JobProjectService;
import com.platform.dts.entity.JobProject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * JobProjectServiceImpl
 * @author AllDataDC
 * @since 2022/11/30
 * @version v2.1.2
 */
@Service("jobProjectService")
public class JobProjectServiceImpl extends ServiceImpl<JobProjectMapper, JobProject> implements JobProjectService {

    @Resource
    private JobProjectMapper jobProjectMapper;

    @Override
    public IPage<JobProject> getProjectListPaging(Integer pageSize, Integer pageNo, String searchName) {
        Page<JobProject> page = new Page(pageNo, pageSize);
        return jobProjectMapper.getProjectListPaging(page, searchName);
    }
}