package com.platform.website.transformer.mr.pv;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * 计算website的page view的reduce类
 */
public class PageViewReducer extends Reducer<StatsUserDimension, NullWritable, StatsUserDimension, MapWritableValue> {
  private MapWritableValue mapWritableValue = new MapWritableValue();
  private MapWritable map = new MapWritable();

  @SuppressWarnings("unused")
  @Override
  protected void reduce(StatsUserDimension key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
    int pvCount = 0;
    for (NullWritable value : values) {
      // pv++，每一条数据算一个pv，不涉及到去重
      pvCount++;
    }

    // 填充value
    this.map.put(new IntWritable(-1), new IntWritable(pvCount));
    this.mapWritableValue.setValue(this.map);

    // 填充kpi
    this.mapWritableValue.setKpi(KpiType.valueOfName(key.getStatsCommon().getKpi().getKpiName()));

    // 输出
    context.write(key, this.mapWritableValue);
  }
}
