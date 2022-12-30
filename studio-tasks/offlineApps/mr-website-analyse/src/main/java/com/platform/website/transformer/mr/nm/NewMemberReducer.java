package com.platform.website.transformer.mr.nm;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * 计算new install user的reduce类
 */
public class NewMemberReducer extends Reducer<StatsUserDimension, TimeOutputValue, StatsUserDimension, MapWritableValue> {
  private Set<String> unique = new HashSet<String>();
  private MapWritableValue outputValue = new MapWritableValue();
  private MapWritable map = new MapWritable();

  @Override
  protected void reduce(StatsUserDimension key, Iterable<TimeOutputValue> values, Context context) throws IOException, InterruptedException {
    for (TimeOutputValue value : values) {
      this.unique.add(value.getId());
    }

    // 输出memberid
    this.outputValue.setKpi(KpiType.INSERT_MEMBER_INFO);
    for (String id : this.unique) {
      this.map.put(new IntWritable(-1), new Text(id));
      this.outputValue.setValue(this.map);
      context.write(key, this.outputValue);
    }

    // value指定
    this.map.put(new IntWritable(-1), new IntWritable(this.unique.size()));
    this.outputValue.setValue(this.map);
    // kpi指定
    this.outputValue.setKpi(KpiType.valueOfName(key.getStatsCommon().getKpi().getKpiName()));
    context.write(key, this.outputValue);
  }
}
