package com.platform.dts.service.impl;

import com.alibaba.fastjson.JSON;
import com.platform.dts.dto.FlinkXJsonBuildDto;
import com.platform.dts.entity.JobDatasource;
import com.platform.dts.service.FlinkxJsonService;
import com.platform.dts.service.JobDatasourceService;
import com.platform.dts.tool.flinkx.FlinkxJsonHelper;
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