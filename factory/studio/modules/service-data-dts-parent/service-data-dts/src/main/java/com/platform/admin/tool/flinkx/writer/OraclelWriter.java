package com.platform.admin.tool.flinkx.writer;

import java.util.Map;

/**
 * oracle writer构建类
 */
public class OraclelWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "oraclewriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
