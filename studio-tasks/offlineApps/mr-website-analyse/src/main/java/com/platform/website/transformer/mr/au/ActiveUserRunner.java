package com.platform.website.transformer.mr.au;

import com.platform.website.common.EventLogConstants;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import com.platform.website.transformer.mr.TransformBaseRunner;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.log4j.Logger;

public class ActiveUserRunner extends TransformBaseRunner {

  private static final Logger logger = Logger.getLogger(ActiveUserRunner.class);

  public static void main(String[] args) {
    ActiveUserRunner runner = new ActiveUserRunner();
    runner.setupRunner("active-user", ActiveUserRunner.class, ActiveUserMapper.class,
        ActiveUserReducer.class, StatsUserDimension.class, TimeOutputValue.class,
        StatsUserDimension.class, MapWritableValue.class);
    try {
      runner.startRunner(args);
    } catch (Exception e) {
      logger.error("运行active user任务出现异常", e);
      throw new RuntimeException(e);
    }
    System.out.println("he");
  }


  @Override
  protected Filter fetchHbaseFilter() {
    FilterList filterList = new FilterList();
    String[] columns = new String[]{
        EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME, EventLogConstants.LOG_COLUMN_NAME_UUID,
        EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME, EventLogConstants.LOG_COLUMN_NAME_PLATFORM,
        EventLogConstants.LOG_COLUMN_NAME_BROWSER_NAME,
        EventLogConstants.LOG_COLUMN_NAME_BROWSER_VERSION
    };
    //过滤数据
    filterList.addFilter(this.getColumnFilter(columns));
    return filterList;
  }

}
