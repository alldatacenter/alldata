package com.platform.website.transformer.mr.location;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsLocationDimension;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TextsOutputValue;
import com.platform.website.transformer.model.value.reduce.LocationReducerOutputValue;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * 计算location维度指标的reduce类
 */
public class LocationReducer extends Reducer<StatsLocationDimension, TextsOutputValue, StatsLocationDimension, LocationReducerOutputValue> {
  private Set<String> uvs = new HashSet<String>();
  private Map<String, Integer> sessions = new HashMap<String, Integer>();
  private LocationReducerOutputValue outputValue = new LocationReducerOutputValue();

  @Override
  protected void reduce(StatsLocationDimension key, Iterable<TextsOutputValue> values, Context context) throws IOException, InterruptedException {
    try {
      for (TextsOutputValue value : values) {
        String uuid = value.getUuid();
        String sid = value.getSid();

        // 将uuid添加的uvs集合中
        this.uvs.add(uuid);
        // 将sid添加到sessions集合中
        if (this.sessions.containsKey(sid)) {
          // 表示该sid已经有访问过的数据
          this.sessions.put(sid, 2);
        } else {
          // 表示该sid是第一次访问
          this.sessions.put(sid, 1);
        }
      }

      // 输出对象的创建
      this.outputValue.setKpi(KpiType.valueOfName(key.getStatsCommon().getKpi().getKpiName()));
      this.outputValue.setUvs(this.uvs.size());
      this.outputValue.setVisits(this.sessions.size());
      int bounceNumber = 0;
      for (Map.Entry<String, Integer> entry : this.sessions.entrySet()) {
        if (entry.getValue() == 1) {
          bounceNumber++;
        }
      }
      this.outputValue.setBounceNumber(bounceNumber);

      // 输出
      context.write(key, this.outputValue);
    } finally {
      // 清空操作
      this.uvs.clear();
      this.sessions.clear();
    }

  }
}
