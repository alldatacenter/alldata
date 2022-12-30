package com.platform.website.transformer.mr.inbound.bounce;

import com.platform.website.common.GlobalConstants;
import com.platform.website.transformer.model.dim.StatsInboundDimension;
import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import com.platform.website.transformer.model.value.reduce.InboundBounceReduceValue;
import com.platform.website.transformer.mr.IOutputCollector;
import com.platform.website.transformer.service.rpc.IDimensionConverter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.hadoop.conf.Configuration;

public class InboundBounceCollector implements IOutputCollector {


  @Override
  public void collect(Configuration conf, BaseDimension key, BaseStatsValueWritable value,
      PreparedStatement preparedStatement, IDimensionConverter converter)
      throws SQLException, IOException {
    StatsInboundDimension inboundDimension = (StatsInboundDimension) key;
    InboundBounceReduceValue inboundReduceValue = (InboundBounceReduceValue) value;
    int i = 0;
    preparedStatement.setInt(++i,
        converter.getDimensionIdByValue(inboundDimension.getStatsCommon().getPlatform()));
    preparedStatement
        .setInt(++i, converter.getDimensionIdByValue(inboundDimension.getStatsCommon().getDate()));
    preparedStatement.setInt(++i, inboundDimension.getInbound().getId());
    preparedStatement.setInt(++i, inboundReduceValue.getBounceNumber());
    preparedStatement.setString(++i, conf.get(GlobalConstants.RUNNING_DATE_PARAMS));
    preparedStatement.setInt(++i, inboundReduceValue.getBounceNumber());

    preparedStatement.addBatch();
  }
}
