package com.platform.dts.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.platform.dts.admin.dto.FlinkXJsonBuildDto;
import com.platform.dts.admin.entity.JobDatasource;
import com.platform.dts.admin.service.FlinkxJsonService;
import com.platform.dts.admin.service.JobDatasourceService;
import com.platform.dts.admin.tool.flinkx.FlinkxJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 *
 * @author AllDataDC
 * @date 2022/11/16 11:14
 * @Description:  JSON构建实现类
 **/
@Service
public class FlinkxJsonServiceImpl implements FlinkxJsonService {

    @Resource
    private JobDatasourceService jobJdbcDatasourceService;

    @Override
    public String buildJobJson(FlinkXJsonBuildDto FlinkXJsonBuildDto) {
        FlinkxJsonHelper flinkxJsonHelper = new FlinkxJsonHelper();
        // reader
        JobDatasource readerDatasource = jobJdbcDatasourceService.getById(FlinkXJsonBuildDto.getReaderDatasourceId());
        flinkxJsonHelper.initReader(FlinkXJsonBuildDto, readerDatasource);
        // writer
        JobDatasource writerDatasource = jobJdbcDatasourceService.getById(FlinkXJsonBuildDto.getWriterDatasourceId());
        flinkxJsonHelper.initWriter(FlinkXJsonBuildDto, writerDatasource);

        return JSON.toJSONString(flinkxJsonHelper.buildJob());
    }
}