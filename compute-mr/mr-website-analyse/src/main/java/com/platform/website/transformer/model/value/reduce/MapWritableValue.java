package com.platform.website.transformer.model.value.reduce;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.WritableUtils;

@Data
public class MapWritableValue extends BaseStatsValueWritable {

  private MapWritable value = new MapWritable();
  private KpiType kpi;

  public MapWritableValue() {
  }

  public MapWritableValue(MapWritable value, KpiType kpi) {
    this.value = value;
    this.kpi = kpi;
  }

  @Override
  public KpiType getKpi() {
    return this.kpi;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    this.value.write(dataOutput);
    WritableUtils.writeEnum(dataOutput, this.kpi);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.value.readFields(dataInput);
    this.kpi = WritableUtils.readEnum(dataInput, KpiType.class);
  }
}
