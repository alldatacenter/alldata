package com.platform.admin.tool.flinkx.writer;

import java.util.Map;

/**
 * oracle writer构建类
 *
 * @author zxl
 * @version 1.0
 * @since 2022/10/15
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
