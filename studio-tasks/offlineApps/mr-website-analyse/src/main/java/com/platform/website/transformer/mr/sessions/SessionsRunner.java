package com.platform.website.transformer.mr.sessions;

import com.platform.website.common.EventLogConstants;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import com.platform.website.transformer.mr.TransformBaseRunner;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.log4j.Logger;

public class SessionsRunner extends TransformBaseRunner {
  private static final Logger logger = Logger.getLogger(SessionsRunner.class);

  public static void main(String[] args) {
    SessionsRunner runner = new SessionsRunner();
    runner.setupRunner("sessions", SessionsRunner.class, SessionsMapper.class, SessionsReducer.class, StatsUserDimension.class, TimeOutputValue.class, StatsUserDimension.class, MapWritableValue.class);
    try {
      runner.startRunner(args);
    } catch (Exception e) {
      logger.error("运行计算session的mapreduce任务出现异常", e);
      throw new RuntimeException("执行任务失败", e);
    }
  }

  @Override
  protected Filter fetchHbaseFilter() {
    FilterList filterList = new FilterList();
    // 定义mapper中需要获取的列名
    String[] columns = new String[] { EventLogConstants.LOG_COLUMN_NAME_SESSION_ID, // 会话id
        EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME, // 服务器时间
        EventLogConstants.LOG_COLUMN_NAME_PLATFORM, // 平台名称
        EventLogConstants.LOG_COLUMN_NAME_BROWSER_NAME, // 浏览器名称
        EventLogConstants.LOG_COLUMN_NAME_BROWSER_VERSION // 浏览器版本号
    };
    filterList.addFilter(this.getColumnFilter(columns));

    return filterList;
  }
}
