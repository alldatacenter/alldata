package com.platform.website.transformer.mr.nu;

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
public class NewInstallUserReducer extends
    Reducer<StatsUserDimension, TimeOutputValue, StatsUserDimension, MapWritableValue> {

  private MapWritableValue outputValue = new MapWritableValue();
  private Set<String> unique = new HashSet<String>();

  @Override
  protected void reduce(StatsUserDimension key, Iterable<TimeOutputValue> values, Context context)
      throws IOException, InterruptedException {
    this.unique.clear();
    //开始计算uuid的个数
    for (TimeOutputValue value : values) {
      this.unique.add(value.getId());
    }

    MapWritable map = new MapWritable();
    map.put(new IntWritable(-1), new IntWritable(this.unique.size()));

    //设置kpi名称
    String kpiName = key.getStatsCommon().getKpi().getKpiName();
    if (KpiType.NEW_INSTALL_USER.name.equals(kpiName)) {
      //计算stats_user表中的新增用户
      outputValue.setKpi(KpiType.NEW_INSTALL_USER);

    } else if (KpiType.BROWSER_NEW_INSTALL_USER.name.equals(kpiName)) {
      //计算stats_device_browser的新增用户
      outputValue.setKpi(KpiType.BROWSER_NEW_INSTALL_USER);
    }
    outputValue.setValue(map);
    context.write(key, outputValue);
  }


}
