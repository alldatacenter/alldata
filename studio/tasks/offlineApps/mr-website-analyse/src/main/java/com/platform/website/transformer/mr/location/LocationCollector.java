package com.platform.website.transformer.mr.location;

import com.platform.website.common.GlobalConstants;
import com.platform.website.transformer.model.dim.StatsLocationDimension;
import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import com.platform.website.transformer.model.value.reduce.LocationReducerOutputValue;
import com.platform.website.transformer.mr.IOutputCollector;
import com.platform.website.transformer.service.rpc.IDimensionConverter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.hadoop.conf.Configuration;

public class LocationCollector implements IOutputCollector {


  @Override
  public void collect(Configuration conf, BaseDimension key, BaseStatsValueWritable value,
      PreparedStatement preparedStatement, IDimensionConverter converter)
      throws SQLException, IOException {
    StatsLocationDimension locationDimension = (StatsLocationDimension)key;
    LocationReducerOutputValue reducerOutputValue = (LocationReducerOutputValue)value;
    int i = 0;
    preparedStatement.setInt(++i, converter.getDimensionIdByValue(locationDimension.getStatsCommon().getPlatform()));
    preparedStatement.setInt(++i, converter.getDimensionIdByValue(locationDimension.getStatsCommon().getDate()));
    preparedStatement.setInt(++i, converter.getDimensionIdByValue(locationDimension.getLocation()));
    preparedStatement.setInt(++i, reducerOutputValue.getUvs());
    preparedStatement.setInt(++i, reducerOutputValue.getVisits());
    preparedStatement.setInt(++i, reducerOutputValue.getBounceNumber());
    preparedStatement.setString(++i, conf.get(GlobalConstants.RUNNING_DATE_PARAMS));
    preparedStatement.setInt(++i, reducerOutputValue.getUvs());
    preparedStatement.setInt(++i, reducerOutputValue.getVisits());
    preparedStatement.setInt(++i, reducerOutputValue.getBounceNumber());

    preparedStatement.addBatch();
  }
}
