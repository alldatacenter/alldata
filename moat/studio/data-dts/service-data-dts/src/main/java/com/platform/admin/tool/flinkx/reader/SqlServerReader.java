package com.platform.admin.tool.flinkx.reader;

import java.util.Map;

/**
 * sqlserver reader 构建类
 */
public class SqlServerReader extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "sqlserverreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
