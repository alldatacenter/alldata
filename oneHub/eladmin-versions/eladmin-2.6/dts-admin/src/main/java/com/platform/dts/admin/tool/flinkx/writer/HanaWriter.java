package com.platform.dts.admin.tool.flinkx.writer;

import java.util.Map;

/**
 * oracle writer构建类
 *
 * @author AllDataDC
 * @version 1.0
 * @date 2022/11/15
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
