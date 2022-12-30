package com.alibaba.sreworks.pmdb.services.datasource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.datasource.DataSourceService;
import com.alibaba.sreworks.pmdb.common.constant.Constant;
import com.alibaba.sreworks.pmdb.common.exception.DataSourceNotExistException;
import com.alibaba.sreworks.pmdb.domain.metric.DataSource;
import com.alibaba.sreworks.pmdb.domain.metric.DataSourceExample;
import com.alibaba.sreworks.pmdb.domain.metric.DataSourceMapper;
import com.alibaba.sreworks.pmdb.domain.req.datasource.DataSourceBaseReq;
import com.alibaba.sreworks.pmdb.domain.req.datasource.DataSourceCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.datasource.DataSourceUpdateReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    public JSONObject getDataSourceById(Integer dataSourceId) {
        DataSource dataSource = dataSourceMapper.selectByPrimaryKey(dataSourceId);
        return convertToJSONObject(dataSource);
    }

    @Override
    public List<JSONObject> getDataSourceByApp(String appId) {
        DataSourceExample example =  new DataSourceExample();
        example.createCriteria().andAppIdEqualTo(appId);

        List<DataSource> dataSources = dataSourceMapper.selectByExampleWithBLOBs(example);
        return convertToJSONObjects(dataSources);
    }

    @Override
    public List<JSONObject> getDataSourceWithCommonByApp(String appId) {
        DataSourceExample example =  new DataSourceExample();
        example.createCriteria().andAppIdEqualTo(Constant.SREWORKS_APP_ID);
        if (!appId.equals(Constant.SREWORKS_APP_ID)) {
            example.or().andAppIdEqualTo(appId);
        }

        List<DataSource> dataSources = dataSourceMapper.selectByExampleWithBLOBs(example);
        return convertToJSONObjects(dataSources);
    }

    @Override
    public List<JSONObject> getDataSourceWithCommonByType(String appId, String dataSourceType) {
        DataSourceExample example =  new DataSourceExample();
        example.createCriteria().andAppIdEqualTo(Constant.SREWORKS_APP_ID).andTypeEqualTo(dataSourceType);
        if (!appId.equals(Constant.SREWORKS_APP_ID)) {
            example.or().andAppIdEqualTo(appId).andTypeEqualTo(dataSourceType);
        }

        List<DataSource> dataSources = dataSourceMapper.selectByExampleWithBLOBs(example);
        return convertToJSONObjects(dataSources);
    }

    @Override
    public int createDataSource(DataSourceCreateReq req) {
        DataSource dataSource = buildDataSource(req);
        return dataSourceMapper.insert(dataSource);
    }

    @Override
    public int updateDataSource(DataSourceUpdateReq req) throws Exception {
        DataSource existDataSource = dataSourceMapper.selectByPrimaryKey(req.getId());
        if (existDataSource == null) {
            throw new DataSourceNotExistException("数据源不存在");
        }

        if (StringUtils.isEmpty(req.getType())) {
            req.setType(existDataSource.getType());
        }

        DataSource dataSource = buildDataSource(req);
        dataSource.setId(req.getId());
        dataSource.setGmtCreate(null);

        dataSourceMapper.updateByPrimaryKeySelective(dataSource);

        return existDataSource.getId();
    }

    @Override
    public int deleteDataSourceById(Integer dataSourceId) {
        return dataSourceMapper.deleteByPrimaryKey(dataSourceId);
    }

    private DataSource buildDataSource(DataSourceBaseReq req) {
        DataSource dataSource = new DataSource();

        Date now = new Date();
        dataSource.setGmtCreate(now);
        dataSource.setGmtModified(now);
        dataSource.setName(req.getName());
        dataSource.setType(req.getType());
        dataSource.setConnectConfig(req.getConnectConfig().toJSONString());
        dataSource.setBuildIn(false);
        dataSource.setAppId(req.getAppId());
        dataSource.setCreator(req.getCreator());
        dataSource.setLastModifier(req.getLastModifier());
        dataSource.setDescription(req.getDescription());

        return dataSource;
    }

    @Override
    public JSONObject convertToJSONObject(Object obj) {
        if (obj == null) {
            return new JSONObject();
        }

        JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(obj));

        String connectConfigStr = result.getString("connectConfig");
        if (StringUtils.isNotEmpty(connectConfigStr)) {
            result.put("connectConfig", JSONObject.parseObject(connectConfigStr));
        }

        return result;
    }

    @Override
    public List<JSONObject> convertToJSONObjects(List<? extends Object> objList) {
        if (objList == null || objList.isEmpty()) {
            return new ArrayList<>();
        }

        return Collections.synchronizedList(objList).parallelStream().map(this::convertToJSONObject).collect(Collectors.toList());
    }
}
