package com.platform.website.transformer.mr.pv;

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

public class PageViewCollector implements IOutputCollector {

  @Override
  public void collect(Configuration conf, BaseDimension key, BaseStatsValueWritable value, PreparedStatement pstmt, IDimensionConverter converter) throws SQLException, IOException {
    StatsUserDimension statsUser = (StatsUserDimension) key;
    int pv = ((IntWritable) ((MapWritableValue) value).getValue().get(new IntWritable(-1))).get();

    // 参数设置
    int i = 0;
    pstmt.setInt(++i, converter.getDimensionIdByValue(statsUser.getStatsCommon().getPlatform()));
    pstmt.setInt(++i, converter.getDimensionIdByValue(statsUser.getStatsCommon().getDate()));
    pstmt.setInt(++i, converter.getDimensionIdByValue(statsUser.getBrowser()));
    pstmt.setInt(++i, pv);
    pstmt.setString(++i, conf.get(GlobalConstants.RUNNING_DATE_PARAMS));
    pstmt.setInt(++i, pv);

    // 加入batch
    pstmt.addBatch();
  }

}
