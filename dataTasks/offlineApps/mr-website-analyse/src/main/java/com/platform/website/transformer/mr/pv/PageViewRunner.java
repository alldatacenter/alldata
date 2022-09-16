package com.platform.website.transformer.mr.pv;

import com.platform.website.common.EventLogConstants;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import com.platform.website.transformer.mr.TransformBaseRunner;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.log4j.Logger;

public class PageViewRunner extends TransformBaseRunner {

  private static final Logger logger = Logger.getLogger(PageViewRunner.class);

  public static void main(String[] args) {
    PageViewRunner runner = new PageViewRunner();
    runner.setupRunner("website_pageview", PageViewRunner.class, PageViewMapper.class,
        PageViewReducer.class, StatsUserDimension.class, NullWritable.class,
        StatsUserDimension.class, MapWritableValue.class);
    try {
      runner.startRunner(args);
    } catch (Exception e) {
      logger.error("计算pv任务出现异常", e);
      throw new RuntimeException("运行job异常", e);
    }
  }

  @Override
  protected Filter fetchHbaseFilter() {
    FilterList filterList = new FilterList();
    // 只需要pageview事件
    filterList.addFilter(
        new SingleColumnValueFilter(Bytes.toBytes(EventLogConstants.EVENT_LOGS_FAMILY_NAME),
            Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME), CompareOp.EQUAL,
            Bytes.toBytes(EventLogConstants.EventEnum.PAGEVIEW.alias)));
    // 定义mapper中需要获取的列名
    String[] columns = new String[]{EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME, // 获取事件名称
        EventLogConstants.LOG_COLUMN_NAME_CURRENT_URL, // 当前url
        EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME, // 服务器时间
        EventLogConstants.LOG_COLUMN_NAME_PLATFORM, // 平台名称
        EventLogConstants.LOG_COLUMN_NAME_BROWSER_NAME, // 浏览器名称
        EventLogConstants.LOG_COLUMN_NAME_BROWSER_VERSION // 浏览器版本号
    };
    filterList.addFilter(this.getColumnFilter(columns));

    return filterList;
  }
}
