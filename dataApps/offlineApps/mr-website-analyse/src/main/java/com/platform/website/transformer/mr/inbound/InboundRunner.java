package com.platform.website.transformer.mr.inbound;

import com.platform.website.common.EventLogConstants;
import com.platform.website.common.EventLogConstants.EventEnum;
import com.platform.website.transformer.model.dim.StatsInboundDimension;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TextsOutputValue;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.model.value.reduce.InboundReduceValue;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import com.platform.website.transformer.mr.TransformBaseRunner;
import com.platform.website.transformer.mr.au.ActiveUserMapper;
import com.platform.website.transformer.mr.au.ActiveUserReducer;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueExcludeFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

/**
 * 计算活跃会员和总会话的入口类
 */
public class InboundRunner extends TransformBaseRunner {

  private static final Logger logger = Logger.getLogger(InboundRunner.class);

  public static void main(String[] args) {
    InboundRunner runner = new InboundRunner();
    runner.setupRunner("inbound", InboundRunner.class, InboundMapper.class,
        InboundReducer.class, StatsInboundDimension.class, TextsOutputValue.class,
        StatsInboundDimension.class, InboundReduceValue.class);
    try {
      runner.startRunner(args);
    } catch (Exception e) {
      logger.error("计算活跃会员和总会话的入口类出现异常", e);
      throw new RuntimeException("计算活跃会员和总会话的入口类出现异常", e);
    }
  }


  @Override
  protected Filter fetchHbaseFilter() {
    FilterList filterList = new FilterList();
    String[] columns = new String[]{
        EventLogConstants.LOG_COLUMN_NAME_REFERER_URL,
        EventLogConstants.LOG_COLUMN_NAME_UUID,
        EventLogConstants.LOG_COLUMN_NAME_SESSION_ID,
        EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME,
        EventLogConstants.LOG_COLUMN_NAME_PLATFORM,
        EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME,
    };
    //过滤数据
    filterList.addFilter(this.getColumnFilter(columns));
    filterList.addFilter(
        new SingleColumnValueExcludeFilter(Bytes.toBytes(EventLogConstants.EVENT_LOGS_FAMILY_NAME),
            Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME), CompareOp.EQUAL,
            Bytes.toBytes(
                EventEnum.PAGEVIEW.alias)));
    return filterList;
  }

}
