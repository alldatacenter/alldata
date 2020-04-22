package com.platform.website.transformer.model.dim.base;

import com.platform.website.transformer.model.dim.StatsCommonDimension;
import com.platform.website.transformer.model.dim.StatsDimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;

@Data
public class StatsInboundBounceDimension extends StatsDimension {

  private StatsCommonDimension statsCommon = new StatsCommonDimension();
  private String sid;
  private long serverTime;

  public StatsInboundBounceDimension() {
    super();
  }

  public StatsInboundBounceDimension(
      StatsCommonDimension statsCommon, String sid, long serverTime) {
    super();
    this.statsCommon = statsCommon;
    this.sid = sid;
    this.serverTime = serverTime;
  }

  public static StatsInboundBounceDimension clone(StatsInboundBounceDimension dimension) {
    return new StatsInboundBounceDimension(StatsCommonDimension.clone(dimension.statsCommon),
        dimension.sid, dimension.serverTime);
  }

  @Override
  public int compareTo(BaseDimension o) {
    if (this == o) {
      return 0;
    }
    StatsInboundBounceDimension other = (StatsInboundBounceDimension) o;
    int tmp = this.statsCommon.compareTo(other.statsCommon);
    if (tmp != 0) {
      return tmp;

    }

    tmp = this.sid.compareTo(other.sid);
    if (tmp != 0) {
      return tmp;

    }
    tmp = Long.compare(this.serverTime, other.serverTime);

    return tmp;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    this.statsCommon.write(dataOutput);
    dataOutput.writeUTF(this.sid);
    dataOutput.writeLong(this.serverTime);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.statsCommon.readFields(dataInput);
    this.sid = dataInput.readUTF();
    this.serverTime = dataInput.readLong();
  }
}
