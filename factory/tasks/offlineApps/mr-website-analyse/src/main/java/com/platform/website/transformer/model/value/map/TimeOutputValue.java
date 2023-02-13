package com.platform.website.transformer.model.value.map;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;

@Data
public class TimeOutputValue extends BaseStatsValueWritable {
  private String id;
  private long time;

  private KpiType kpiType;


  @Override
  public KpiType getKpi() {
    return this.kpiType;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeUTF(this.id);
    dataOutput.writeLong(this.time);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.id = dataInput.readUTF();
    this.time = dataInput.readLong();
  }
}
