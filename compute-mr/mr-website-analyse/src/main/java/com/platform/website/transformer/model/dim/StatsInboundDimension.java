package com.platform.website.transformer.model.dim;

import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.dim.base.InboundDimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;

@Data
public class StatsInboundDimension extends StatsDimension {

  private StatsCommonDimension statsCommon = new StatsCommonDimension();
  private InboundDimension inbound = new InboundDimension();

  public static StatsInboundDimension clone(StatsInboundDimension dimension) {
    return new StatsInboundDimension(StatsCommonDimension.clone(dimension.statsCommon),
        new InboundDimension(dimension.inbound))
        ;
  }


  public StatsInboundDimension() {
    super();
  }

  public StatsInboundDimension(StatsCommonDimension statsCommon,
      InboundDimension inbound) {
    super();
    this.statsCommon = statsCommon;
    this.inbound = inbound;
  }

  @Override
  public int compareTo(BaseDimension o) {
    if (this == o) {
      return 0;
    }
    StatsInboundDimension other = (StatsInboundDimension) o;
    int tmp = this.statsCommon.compareTo(other.statsCommon);
    if (tmp != 0) {
      return tmp;
    }

    tmp = this.inbound.compareTo(other.inbound);
    return tmp;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    this.statsCommon.write(dataOutput);
    this.inbound.write(dataOutput);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.statsCommon.readFields(dataInput);
    this.inbound.readFields(dataInput);
  }

}
