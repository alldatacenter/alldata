package com.alibaba.sreworks.dataset.services.datasource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.datasource.DataSourceService;
import com.alibaba.sreworks.dataset.domain.pmdb.DataSource;
import com.alibaba.sreworks.dataset.domain.pmdb.DataSourceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 数据源Service
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/10 11:13
 */
@Slf4j
@Service
public class DataSourceServiceImpl implements DataSourceService {

    @Autowired
    DataSourceMapper dataSourceMapper;

    @Override
    public JSONObject getDataSourceById(String dataSourceId) {
        DataSource dataSource = dataSourceMapper.selectByPrimaryKey(Integer.parseInt(dataSourceId));
        return convertToJSONObject(dataSource);
    }
}
