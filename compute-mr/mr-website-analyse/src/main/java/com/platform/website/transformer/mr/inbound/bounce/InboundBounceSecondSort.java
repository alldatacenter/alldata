package com.platform.website.transformer.mr.inbound.bounce;

import com.platform.website.transformer.model.dim.StatsCommonDimension;
import com.platform.website.transformer.model.dim.base.StatsInboundBounceDimension;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;

/**
 * 自定义的二次排序用到的类
 */
public class InboundBounceSecondSort {

  /**
   * 自定义分组类
   */
  public static class InboundBounceGroupingComparator extends WritableComparator{
    public InboundBounceGroupingComparator(){
      super(StatsInboundBounceDimension.class, true);
    }

    @Override
    public int compare(Object a, Object b){
      StatsInboundBounceDimension key1 = (StatsInboundBounceDimension) a;
      StatsInboundBounceDimension key2 = (StatsInboundBounceDimension) b;
      return key1.getStatsCommon().compareTo(key2.getStatsCommon());
    }
  }


  /**
   * 自定义reduce分组函数
   */
  public static class InboundBouncePartitioner extends
      Partitioner<StatsInboundBounceDimension, IntWritable> {
    private HashPartitioner<StatsCommonDimension, IntWritable> partitioner = new HashPartitioner<>();


    @Override
    public int getPartition(StatsInboundBounceDimension key, IntWritable value, int numPartitions) {
      return this.partitioner.getPartition(key.getStatsCommon(), value, numPartitions);
    }
  }

}
