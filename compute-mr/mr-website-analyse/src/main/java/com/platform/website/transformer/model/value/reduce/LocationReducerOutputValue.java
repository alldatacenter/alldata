package com.platform.website.transformer.model.value.reduce;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;
import org.apache.hadoop.io.WritableUtils;

@Data
public class LocationReducerOutputValue extends BaseStatsValueWritable {

  private KpiType kpi;
  private int uvs; //活跃用户数
  private int visits;//会话个数
  private int bounceNumber;//跳出会话个数

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(this.uvs);
    dataOutput.writeInt(this.visits);
    dataOutput.writeInt(this.bounceNumber);
    WritableUtils.writeEnum(dataOutput, this.kpi);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.uvs  = dataInput.readInt();
    this.visits = dataInput.readInt();
    this.bounceNumber = dataInput.readInt();
    this.kpi = WritableUtils.readEnum(dataInput, KpiType.class);
  }

}
