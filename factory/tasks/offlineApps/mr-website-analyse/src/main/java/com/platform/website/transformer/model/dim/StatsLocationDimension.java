package com.platform.website.transformer.model.dim;

import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.dim.base.LocationDimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;

@Data
public class StatsLocationDimension extends StatsDimension{
  private StatsCommonDimension statsCommon = new StatsCommonDimension();
  private LocationDimension location = new LocationDimension();

  public static StatsLocationDimension clone(StatsLocationDimension dimension){
    StatsLocationDimension newDimension = new StatsLocationDimension();
    newDimension.statsCommon = StatsCommonDimension.clone(dimension.statsCommon);
    newDimension.location = LocationDimension.newInstance(dimension.location.getCountry(), dimension.location.getProvince(), dimension.location.getCity());
    newDimension.location.setId(dimension.location.getId());
    return newDimension;
  }

  public StatsLocationDimension() {
    super();
  }

  public StatsLocationDimension(StatsCommonDimension statsCommon,
      LocationDimension location) {
    super();
    this.statsCommon = statsCommon;
    this.location = location;
  }

  @Override
  public int compareTo(BaseDimension o) {
    if (this == o) {
      return 0;
    }
    StatsLocationDimension other = (StatsLocationDimension) o;
    int tmp = this.statsCommon.compareTo(other.statsCommon);
    if (tmp != 0) {
      return tmp;
    }

    tmp = this.location.compareTo(other.location);
    return tmp;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    this.statsCommon.write(dataOutput);
    this.location.write(dataOutput);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.statsCommon.readFields(dataInput);
    this.location.readFields(dataInput);
  }
}
