package com.platform.website.transformer.mr.inbound.bounce;

import com.platform.website.common.EventLogConstants;
import com.platform.website.common.EventLogConstants.EventEnum;
import com.platform.website.transformer.model.dim.StatsInboundDimension;
import com.platform.website.transformer.model.dim.base.StatsInboundBounceDimension;
import com.platform.website.transformer.model.value.map.TextsOutputValue;
import com.platform.website.transformer.model.value.reduce.InboundBounceReduceValue;
import com.platform.website.transformer.model.value.reduce.InboundReduceValue;
import com.platform.website.transformer.mr.TransformBaseRunner;
import com.platform.website.transformer.mr.inbound.InboundMapper;
import com.platform.website.transformer.mr.inbound.InboundReducer;
import com.platform.website.transformer.mr.inbound.bounce.InboundBounceSecondSort.InboundBounceGroupingComparator;
import com.platform.website.transformer.mr.inbound.bounce.InboundBounceSecondSort.InboundBouncePartitioner;
import java.io.IOException;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueExcludeFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.log4j.Logger;

/**
 * 计算inbound 跳出率的入口类
 */
public class InboundBounceRunner extends TransformBaseRunner {

  private static final Logger logger = Logger.getLogger(InboundBounceRunner.class);

  public static void main(String[] args) {
    InboundBounceRunner runner = new InboundBounceRunner();
    runner.setupRunner("inbound_bounce", InboundBounceRunner.class, InboundBounceMapper.class,
        InboundBounceReducer.class, StatsInboundBounceDimension.class, IntWritable.class,
        StatsInboundDimension.class, InboundBounceReduceValue.class);
    try {
      runner.startRunner(args);
    } catch (Exception e) {
      logger.error("计算活跃会员和总会话的入口类出现异常", e);
      throw new RuntimeException("计算活跃会员和总会话的入口类出现异常", e);
    }
  }

@Override
protected void beforeRunJob(Job job) throws IOException{
    super.beforeRunJob(job);
    job.setGroupingComparatorClass(InboundBounceGroupingComparator.class);
    job.setPartitionerClass(InboundBouncePartitioner.class);
}

  @Override
  protected Filter fetchHbaseFilter() {
    FilterList filterList = new FilterList();
    String[] columns = new String[]{
        EventLogConstants.LOG_COLUMN_NAME_REFERER_URL,
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
