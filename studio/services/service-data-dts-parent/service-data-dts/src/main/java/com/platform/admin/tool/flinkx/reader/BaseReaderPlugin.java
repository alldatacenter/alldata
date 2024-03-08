package com.platform.admin.tool.flinkx.reader;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.platform.admin.entity.JobDatasource;
import com.platform.admin.tool.flinkx.BaseFlinkxPlugin;
import com.platform.admin.tool.pojo.FlinkxHbasePojo;
import com.platform.admin.tool.pojo.FlinkxHivePojo;
import com.platform.admin.tool.pojo.FlinkxMongoDBPojo;
import com.platform.admin.tool.pojo.FlinkxRdbmsPojo;
import com.platform.admin.util.AESUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 读插件
 **/
public abstract class BaseReaderPlugin extends BaseFlinkxPlugin {


    @Override
    public Map<String, Object> build(FlinkxRdbmsPojo plugin) {
        //构建
        Map<String, Object> readerObj = Maps.newLinkedHashMap();
        readerObj.put("name", getName());
        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
        Map<String, Object> connectionObj = Maps.newLinkedHashMap();

        JobDatasource jobDatasource = plugin.getJobDatasource();
        //将用户名和密码进行解密
        parameterObj.put("username", AESUtil.decrypt(jobDatasource.getJdbcUsername()));
        parameterObj.put("password", AESUtil.decrypt(jobDatasource.getJdbcPassword()));

        //判断是否是 querySql
        if (StrUtil.isNotBlank(plugin.getQuerySql())) {
            connectionObj.put("querySql", ImmutableList.of(plugin.getQuerySql()));
        } else {
            parameterObj.put("column", plugin.getRdbmsColumns());
            //判断是否有where
            if (StringUtils.isNotBlank(plugin.getWhereParam())) {
                parameterObj.put("where", plugin.getWhereParam());
            }
            connectionObj.put("table", plugin.getTables());
        }
        parameterObj.put("splitPk",plugin.getSplitPk());
        connectionObj.put("jdbcUrl", ImmutableList.of(jobDatasource.getJdbcUrl()));

        parameterObj.put("connection", ImmutableList.of(connectionObj));

        readerObj.put("parameter", parameterObj);

        return readerObj;
    }

    @Override
    public Map<String, Object> buildHive(FlinkxHivePojo flinkxHivePojo) {
        return null;
    }

    @Override
    public Map<String, Object> buildHbase(FlinkxHbasePojo flinkxHbasePojo) { return null; }

    @Override
    public Map<String, Object> buildMongoDB(FlinkxMongoDBPojo flinkxMongoDBPojo) {
        return null;
    }
}
