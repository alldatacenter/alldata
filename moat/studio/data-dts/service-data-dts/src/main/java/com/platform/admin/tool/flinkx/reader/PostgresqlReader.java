package com.platform.admin.tool.flinkx.reader;

import java.util.Map;

/**
 * postgresql 构建类
 */
public class PostgresqlReader extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "postgresqlreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
