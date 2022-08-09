package com.platform.website.transformer.model.dim;

import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.dim.base.DateDimension;
import com.platform.website.transformer.model.dim.base.KpiDimension;
import com.platform.website.transformer.model.dim.base.PlatformDimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;

/**
 * 公用的dimension信息组合
 */

@Data
public class StatsCommonDimension extends StatsDimension {

  private DateDimension date = new DateDimension();
  private PlatformDimension platform = new PlatformDimension();
  private KpiDimension kpi = new KpiDimension();

  public StatsCommonDimension() {
  }

  public StatsCommonDimension(DateDimension date,
      PlatformDimension platform, KpiDimension kpi) {
    super();
    this.date = date;
    this.platform = platform;
    this.kpi = kpi;
  }


  public static StatsCommonDimension clone(StatsCommonDimension dimension) {
    DateDimension date = new DateDimension(dimension.date.getId(),
        dimension.date.getYear(), dimension.date.getSeason(), dimension.date.getMonth(),
        dimension.date.getWeek(),
        dimension.date.getDay(), dimension.date.getType(), dimension.date.getCalendar());
    PlatformDimension platform = new PlatformDimension(dimension.platform.getId(),
        dimension.platform.getPlatformName());
    KpiDimension kpi = new KpiDimension(dimension.kpi.getId(), dimension.kpi.getKpiName());
    return new StatsCommonDimension(date, platform, kpi);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    StatsCommonDimension that = (StatsCommonDimension) o;

    if (!date.equals(that.date)) {
      return false;
    }
    if (!platform.equals(that.platform)) {
      return false;
    }
    return kpi.equals(that.kpi);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + date.hashCode();
    result = 31 * result + platform.hashCode();
    result = 31 * result + kpi.hashCode();
    return result;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    this.date.write(out);
    this.platform.write(out);
    this.kpi.write(out);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.date.readFields(dataInput);
    this.platform.readFields(dataInput);
    this.kpi.readFields(dataInput);
  }

  @Override
  public int compareTo(BaseDimension o) {
    if (this == o) {
      return 0;
    }
    StatsCommonDimension other = (StatsCommonDimension) o;
    int tmp = this.date.compareTo(other.date);
    if (tmp != 0) {
      return tmp;

    }

    tmp = this.platform.compareTo(other.platform);
    if (tmp != 0) {
      return tmp;

    }
    tmp = this.kpi.compareTo(other.kpi);

    return tmp;
  }
}
