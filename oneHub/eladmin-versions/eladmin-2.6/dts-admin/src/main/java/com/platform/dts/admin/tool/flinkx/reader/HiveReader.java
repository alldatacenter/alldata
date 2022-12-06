package com.platform.dts.admin.tool.flinkx.reader;

import com.google.common.collect.Maps;
import com.platform.dts.admin.tool.pojo.FlinkxHivePojo;

import java.util.Map;

/**
 * hive reader 构建类
 *
 * @author AllDataDC
 * @version 2.0
 * @date 2022/11/05
 */
public class HiveReader extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "hdfsreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }


    @Override
    public Map<String, Object> buildHive(FlinkxHivePojo plugin) {
        //构建
        Map<String, Object> readerObj = Maps.newLinkedHashMap();
        readerObj.put("name", getName());
        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
        parameterObj.put("path", plugin.getReaderPath());
        parameterObj.put("defaultFS", plugin.getReaderDefaultFS());
        parameterObj.put("fileType", plugin.getReaderFileType());
        parameterObj.put("fieldDelimiter", plugin.getReaderFieldDelimiter());
        parameterObj.put("skipHeader", plugin.getSkipHeader());
        parameterObj.put("column", plugin.getColumns());
        readerObj.put("parameter", parameterObj);
        return readerObj;
    }
}
