package com.platform.admin.tool.flinkx.reader;

import com.google.common.collect.Maps;
import com.platform.admin.tool.pojo.FlinkxHbasePojo;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class HBaseReader extends BaseReaderPlugin implements FlinkxReaderInterface {
  @Override
  public String getName() {
    return "hbasereader";
  }

  @Override
  public Map<String, Object> sample() {
    return null;
  }

  public Map<String, Object> buildHbase(FlinkxHbasePojo plugin) {
    //构建
    Map<String, Object> readerObj = Maps.newLinkedHashMap();
    readerObj.put("name", getName());
    Map<String, Object> parameterObj = Maps.newLinkedHashMap();
    Map<String, Object> confige = Maps.newLinkedHashMap();
	confige.put("hbase.zookeeper.property.clientPort", plugin.getReaderHbaseConfig().split(":")[1]);
	  //		confige.put("hbase.rootdir", plugin.getWriterHbaseConfig());
	confige.put("hbase.cluster.distributed", "true");
	confige.put("hbase.zookeeper.quorum", plugin.getReaderHbaseConfig().split(":")[0]);
	confige.put("zookeeper.znode.parent", "/hbase");
    parameterObj.put("hbaseConfig", confige);
    parameterObj.put("table", plugin.getReaderTable());
    parameterObj.put("mode", plugin.getReaderMode());
    parameterObj.put("column", plugin.getColumns());
    if(StringUtils.isNotBlank(plugin.getReaderRange().getStartRowkey()) && StringUtils.isNotBlank(plugin.getReaderRange().getEndRowkey())){
      parameterObj.put("range", plugin.getReaderRange());
    }
    parameterObj.put("maxVersion", plugin.getReaderMaxVersion());
    readerObj.put("parameter", parameterObj);
    return readerObj;
  }
}
