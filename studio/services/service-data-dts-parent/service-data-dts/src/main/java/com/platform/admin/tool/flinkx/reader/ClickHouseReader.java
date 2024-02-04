package com.platform.admin.tool.flinkx.reader;

import java.util.Map;

public class ClickHouseReader  extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "clickhousereader";
    }


    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
