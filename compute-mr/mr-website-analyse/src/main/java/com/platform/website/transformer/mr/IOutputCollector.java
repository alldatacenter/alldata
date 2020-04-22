package com.platform.website.transformer.mr;

import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import com.platform.website.transformer.service.rpc.IDimensionConverter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.hadoop.conf.Configuration;

/**
 * 自定义的配合自定义output进行具体sql输出的类
 */
public interface IOutputCollector {

  /**
   * 具体执行数据插入的方法
   * @param conf
   * @param key
   * @param value
   * @param converter
   * @throws SQLException
   */
  public void collect(Configuration conf, BaseDimension key, BaseStatsValueWritable value, PreparedStatement preparedStatement, IDimensionConverter converter)
      throws SQLException, IOException;
}
