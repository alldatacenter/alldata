package com.platform.admin.tool.flinkx.writer;


import java.util.Map;

/**
 * sql server writer构建类
 */
public class SqlServerlWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "sqlserverwriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
