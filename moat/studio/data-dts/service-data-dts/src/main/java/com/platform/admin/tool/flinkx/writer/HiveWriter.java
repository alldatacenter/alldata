package com.platform.admin.tool.flinkx.writer;

import com.google.common.collect.Maps;
import com.platform.admin.tool.pojo.FlinkxHivePojo;

import java.util.Map;

/**
 * hive writer构建类
 *
 * @author AllDataDC
 * @date 2022/01/05
 */
public class HiveWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "hdfswriter";
    }


    @Override
    public Map<String, Object> sample() {
        return null;
    }

    @Override
    public Map<String, Object> buildHive(FlinkxHivePojo plugin) {
        Map<String, Object> writerObj = Maps.newLinkedHashMap();
        writerObj.put("name", getName());

        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
        parameterObj.put("defaultFS", plugin.getWriterDefaultFS());
        parameterObj.put("fileType", plugin.getWriterFileType());
        parameterObj.put("path", plugin.getWriterPath());
        parameterObj.put("fileName", plugin.getWriterFileName());
        parameterObj.put("writeMode", plugin.getWriteMode());
        parameterObj.put("fieldDelimiter", plugin.getWriteFieldDelimiter());
        parameterObj.put("column", plugin.getColumns());
        writerObj.put("parameter", parameterObj);
        return writerObj;
    }
}
