package com.platform.website.transformer.model.value.reduce;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;
import org.apache.hadoop.io.WritableUtils;

@Data
public class InboundBounceReduceValue extends BaseStatsValueWritable {

  private KpiType kpi;
  private int bounceNumber;

  public InboundBounceReduceValue() {
    super();
  }


  public InboundBounceReduceValue(int bounceNumber) {
    super();
    this.bounceNumber = bounceNumber;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(this.bounceNumber);
    WritableUtils.writeEnum(dataOutput, this.kpi);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.bounceNumber = dataInput.readInt();
    this.kpi = WritableUtils.readEnum(dataInput, KpiType.class);
  }

  /**
   * 自增1
   */
  public void incrBounceNum(){
    this.bounceNumber += 1;
  }
}
