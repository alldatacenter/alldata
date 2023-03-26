package com.platform.admin.tool.flinkx.writer;

import com.google.common.collect.Maps;
import com.platform.admin.tool.pojo.FlinkxHbasePojo;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class HBaseWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "hbasewriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }

    public Map<String, Object> buildHbase(FlinkxHbasePojo plugin) {
        //构建
        Map<String, Object> writerObj = Maps.newLinkedHashMap();
        writerObj.put("name", getName());
        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
        Map<String, Object> confige = Maps.newLinkedHashMap();

        confige.put("hbase.zookeeper.property.clientPort", plugin.getWriterHbaseConfig().split(":")[1]);
//		confige.put("hbase.rootdir", plugin.getWriterHbaseConfig());
		confige.put("hbase.cluster.distributed", "true");
		confige.put("hbase.zookeeper.quorum", plugin.getWriterHbaseConfig().split(":")[0]);
		confige.put("zookeeper.znode.parent", "/hbase");
        parameterObj.put("hbaseConfig", confige);
        parameterObj.put("table", plugin.getWriterTable());
        parameterObj.put("mode", plugin.getWriterMode());
        parameterObj.put("column", plugin.getColumns());
        parameterObj.put("rowkeyColumn", plugin.getWriterRowkeyColumn());
        if (StringUtils.isNotBlank(plugin.getWriterVersionColumn().getValue())) {
            parameterObj.put("versionColumn", plugin.getWriterVersionColumn());
        }
        writerObj.put("parameter", parameterObj);
        return writerObj;
    }
}
