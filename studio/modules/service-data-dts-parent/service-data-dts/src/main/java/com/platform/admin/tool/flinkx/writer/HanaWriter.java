package com.platform.admin.tool.flinkx.writer;

import java.util.Map;

/**
 * oracle writer构建类
 */
public class HanaWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "saphanawriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
