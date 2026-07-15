package com.platform.admin.tool.flinkx.writer;

import java.util.Map;

/**
 * postgresql writer构建类
 */
public class PostgresqllWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "postgresqlwriter";
    }


    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
