package com.platform.dts.admin.tool.flinkx.reader;

import java.util.Map;

/**
 * oracle reader 构建类
 *
 * @author AllDataDC
 * @version 1.0
 * @since 2022/11/2
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
