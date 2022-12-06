package com.platform.dts.admin.tool.flinkx.reader;


import java.util.Map;

/**
 * Hana reader 构建类
 *
 * @author AllDataDC
 * @version 1.0
 * @date 2022/11/15
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
