package com.platform.website.transformer.model.value.reduce;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;
import org.apache.hadoop.io.WritableUtils;

@Data
public class InboundReduceValue extends BaseStatsValueWritable {

  private KpiType kpi;
  private int uvs;
  private int visit;

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(this.uvs);
    dataOutput.writeInt(this.visit);
    WritableUtils.writeEnum(dataOutput, this.kpi);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.uvs = dataInput.readInt();
    this.visit = dataInput.readByte();
    this.kpi = WritableUtils.readEnum(dataInput, KpiType.class);
  }
}
