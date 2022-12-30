package com.platform.website.transformer.mr.au;

import com.platform.website.common.DateEnum;
import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import com.platform.website.util.TimeUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * 计算new install user的reduce类
 */
public class ActiveUserReducer extends Reducer<StatsUserDimension, TimeOutputValue, StatsUserDimension, MapWritableValue> {
  private Set<String> unique = new HashSet<String>();
  private Map<Integer, Set<String>> hourlyUnique = new HashMap<Integer, Set<String>>();
  private MapWritableValue outputValue = new MapWritableValue();
  private MapWritable map = new MapWritable();
  private MapWritable hourlyMap = new MapWritable();

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    super.setup(context);
    // 进行初始化操作
    for (int i = 0; i < 24; i++) {
      this.hourlyMap.put(new IntWritable(i), new IntWritable(0));
      this.hourlyUnique.put(i, new HashSet<String>());
    }
  }

  @Override
  protected void reduce(StatsUserDimension key, Iterable<TimeOutputValue> values, Context context) throws IOException, InterruptedException {
    try {
      String kpiName = key.getStatsCommon().getKpi().getKpiName();
      if (KpiType.HOURLY_ACTIVE_USER.name.equals(kpiName)) {
        // 计算hourly active user
        for (TimeOutputValue value : values) {
          // 计算出访问的小时，从[0,23]的区间段
          int hour = TimeUtil.getDateInfo(value.getTime(), DateEnum.HOUR);
          this.hourlyUnique.get(hour).add(value.getId()); // 将会话id添加到对应的时间段中
        }

        // 设置kpi
        this.outputValue.setKpi(KpiType.HOURLY_ACTIVE_USER);
        // 设置value
        for (Map.Entry<Integer, Set<String>> entry : this.hourlyUnique.entrySet()) {
          this.hourlyMap.put(new IntWritable(entry.getKey()), new IntWritable(entry.getValue().size()));
        }
        this.outputValue.setValue(this.hourlyMap);

        // 输出操作
        context.write(key, this.outputValue);
      } else {
        // 计算active user,分别是stats_user和stats_device_browser表上
        // 将uuid添加到set集合中去，方便进行统计uuid的去重个数
        for (TimeOutputValue value : values) {
          this.unique.add(value.getId());
        }

        // 设置kpi
        this.outputValue.setKpi(KpiType.valueOfName(key.getStatsCommon().getKpi().getKpiName()));
        // 设置value
        this.map.put(new IntWritable(-1), new IntWritable(this.unique.size()));
        this.outputValue.setValue(this.map);

        // 进行输出
        context.write(key, this.outputValue);
      }
    } finally {
      // 清空操作
      this.unique.clear();
      this.map.clear();
      this.hourlyMap.clear();
      this.hourlyUnique.clear();
      // 初始化操作
      for (int i = 0; i < 24; i++) {
        this.hourlyMap.put(new IntWritable(i), new IntWritable(0));
        this.hourlyUnique.put(i, new HashSet<String>());
      }
    }

  }
}
