package com.platform.website.transformer.mr.au;

import com.platform.website.common.GlobalConstants;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import com.platform.website.transformer.mr.IOutputCollector;
import com.platform.website.transformer.service.rpc.IDimensionConverter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;

public class ActiveUserBrowserCollector implements IOutputCollector {


  @Override
  public void collect(Configuration conf, BaseDimension key, BaseStatsValueWritable value,
      PreparedStatement preparedStatement, IDimensionConverter converter)
      throws SQLException, IOException {
    StatsUserDimension statsUser = (StatsUserDimension)key;
    MapWritableValue mapWritableValue = (MapWritableValue)value;
    IntWritable activeUserValue = (IntWritable) mapWritableValue.getValue().get(new IntWritable(-1));

    int i = 0;
    preparedStatement.setInt(++i, converter.getDimensionIdByValue(statsUser.getStatsCommon().getPlatform()));
    preparedStatement.setInt(++i, converter.getDimensionIdByValue(statsUser.getStatsCommon().getDate()));
    preparedStatement.setInt(++i, converter.getDimensionIdByValue(statsUser.getBrowser()));
    preparedStatement.setInt(++i, activeUserValue.get());
    preparedStatement.setString(++i, conf.get(GlobalConstants.RUNNING_DATE_PARAMS));
    preparedStatement.setInt(++i, activeUserValue.get());
    preparedStatement.addBatch();
  }
}
