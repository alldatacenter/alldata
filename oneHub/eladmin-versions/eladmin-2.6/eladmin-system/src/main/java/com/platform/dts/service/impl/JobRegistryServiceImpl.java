package com.platform.dts.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.dts.mapper.JobRegistryMapper;
import com.platform.dts.service.JobRegistryService;
import com.platform.dts.entity.JobRegistry;
import org.springframework.stereotype.Service;

/**
 * JobRegistryServiceImpl
 * @author AllDataDC
 * @since 2022/11/15
 * @version v2.1.1
 */
@Service("jobRegistryService")
public class JobRegistryServiceImpl extends ServiceImpl<JobRegistryMapper, JobRegistry> implements JobRegistryService {

}