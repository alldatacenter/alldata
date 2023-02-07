package com.platform.website.transformer.mr.location;

import com.platform.website.common.EventLogConstants;
import com.platform.website.common.EventLogConstants.EventEnum;
import com.platform.website.transformer.model.dim.StatsLocationDimension;
import com.platform.website.transformer.model.value.map.TextsOutputValue;
import com.platform.website.transformer.model.value.reduce.LocationReducerOutputValue;
import com.platform.website.transformer.mr.TransformBaseRunner;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueExcludeFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

public class LocationRunner extends TransformBaseRunner {

  private static final Logger logger = Logger.getLogger(LocationRunner.class);

  public static void main(String[] args) {
    LocationRunner runner = new LocationRunner();
    runner.setupRunner("location", LocationRunner.class, LocationMapper.class
        , LocationReducer.class, StatsLocationDimension.class, TextsOutputValue.class,
        StatsLocationDimension.class, LocationReducerOutputValue.class);
    try {
      runner.startRunner(args);
    } catch (Exception e) {
      logger.error("运行location维度的统计出现异常", e);
      throw new RuntimeException(e);
    }
    System.out.println("he");
  }



  @Override
  protected Filter fetchHbaseFilter() {
    //过滤数据，只分析launch事件
    FilterList filterList = new FilterList();
    filterList.addFilter(
        new SingleColumnValueExcludeFilter(Bytes.toBytes(EventLogConstants.EVENT_LOGS_FAMILY_NAME),
            Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME), CompareOp.EQUAL,
            Bytes.toBytes(
                EventEnum.PAGEVIEW.alias)));
    String[] columns = new String[]{
        EventLogConstants.LOG_COLUMN_NAME_SESSION_ID, EventLogConstants.LOG_COLUMN_NAME_UUID,
        EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME, EventLogConstants.LOG_COLUMN_NAME_PLATFORM,
        EventLogConstants.LOG_COLUMN_NAME_COUNTRY,
        EventLogConstants.LOG_COLUMN_NAME_PROVINCE,
        EventLogConstants.LOG_COLUMN_NAME_CITY,
        EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME
    };
    filterList.addFilter(this.getColumnFilter(columns));
    return filterList;
  }
}
