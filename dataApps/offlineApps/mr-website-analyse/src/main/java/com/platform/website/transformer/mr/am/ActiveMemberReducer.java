package com.platform.website.transformer.mr.am;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * 计算new install user的reduce类
 */
public class ActiveMemberReducer extends
    Reducer<StatsUserDimension, TimeOutputValue, StatsUserDimension, MapWritableValue> {

  private MapWritableValue outputValue = new MapWritableValue();
  private Set<String> unique = new HashSet<String>();
  private MapWritable map = new MapWritable();

  @Override
  protected void reduce(StatsUserDimension key, Iterable<TimeOutputValue> values, Context context)
      throws IOException, InterruptedException {
    try{
      //开始计算memberId的个数
      for (TimeOutputValue value : values) {
        this.unique.add(value.getId());
      }
      //设置kpi
      outputValue.setKpi(KpiType.valueOfName(key.getStatsCommon().getKpi().getKpiName()));
      //设置value
      this.map.put(new IntWritable(-1), new IntWritable(this.unique.size()));
      outputValue.setValue(this.map);
      context.write(key, outputValue);
    }finally {
      this.unique.clear();
    }

  }


}
