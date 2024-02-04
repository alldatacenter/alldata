package com.platform.admin.tool.flinkx.reader;


import java.util.Map;

/**
 * Hana reader 构建类
 */
public class HanaReader extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "saphanareader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
