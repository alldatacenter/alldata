package com.platform.admin.tool.flinkx.reader;

import com.google.common.collect.Maps;
import com.platform.admin.entity.JobDatasource;
import com.platform.admin.tool.pojo.FlinkxMongoDBPojo;

import java.util.Map;

public class MongoDBReader extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "mongodbreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }

    public Map<String, Object> buildMongoDB(FlinkxMongoDBPojo plugin) {
        //构建
        JobDatasource dataSource = plugin.getJdbcDatasource();
        Map<String, Object> readerObj = Maps.newLinkedHashMap();
        readerObj.put("name", getName());
        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
		parameterObj.put("url", dataSource.getJdbcUrl()+""+dataSource.getDatabaseName());
		parameterObj.put("database",dataSource.getDatabaseName());
        parameterObj.put("collectionName", plugin.getReaderTable());
        parameterObj.put("column", plugin.getColumns());
        readerObj.put("parameter", parameterObj);
        return readerObj;
    }
}
