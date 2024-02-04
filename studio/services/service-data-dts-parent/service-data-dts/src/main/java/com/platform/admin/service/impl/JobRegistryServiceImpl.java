package com.platform.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.admin.mapper.JobRegistryMapper;
import com.platform.admin.service.JobRegistryService;
import com.platform.admin.entity.JobRegistry;
import org.springframework.stereotype.Service;

/**
 * JobRegistryServiceImpl
 * @author AllDataDC
 * @date 2022-03-15
 * @version v2.1.1
 */
@Service("jobRegistryService")
public class JobRegistryServiceImpl extends ServiceImpl<JobRegistryMapper, JobRegistry> implements JobRegistryService {

}