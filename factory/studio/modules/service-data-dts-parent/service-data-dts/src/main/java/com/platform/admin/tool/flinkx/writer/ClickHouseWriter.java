package com.platform.admin.tool.flinkx.writer;

import java.util.Map;

public class ClickHouseWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "clickhousewriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
