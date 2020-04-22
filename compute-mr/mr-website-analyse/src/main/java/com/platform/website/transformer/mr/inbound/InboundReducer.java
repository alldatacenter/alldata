package com.platform.website.transformer.mr.inbound;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsInboundDimension;
import com.platform.website.transformer.model.value.map.TextsOutputValue;
import com.platform.website.transformer.model.value.reduce.InboundReduceValue;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * 计算Inbound外链维度指标的reduce类
 */
public class InboundReducer extends
    Reducer<StatsInboundDimension, TextsOutputValue, StatsInboundDimension, InboundReduceValue> {

  private Set<String> uvs = new HashSet<String>();
  private Set<String> visits = new HashSet<String>();
  private InboundReduceValue outputValue = new InboundReduceValue();

  @Override
  protected void reduce(StatsInboundDimension key, Iterable<TextsOutputValue> values, Context context)
      throws IOException, InterruptedException {
    try {
      for (TextsOutputValue value : values) {
        this.uvs.add(value.getUuid());
        this.visits.add(value.getSid());
      }

      this.outputValue.setKpi(KpiType.valueOfName(key.getStatsCommon().getKpi().getKpiName()));
      this.outputValue.setUvs(this.uvs.size());
      this.outputValue.setVisit(this.visits.size());
      context.write(key, this.outputValue);
    }finally {
      this.uvs.clear();
      this.visits.clear();
    }

  }


}
