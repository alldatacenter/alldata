package com.platform.admin.tool.flinkx.reader;

import java.util.Map;

/**
 * oracle reader 构建类
 */
public class OracleReader extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "oraclereader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
