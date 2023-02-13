package com.platform.website.transformer.model.dim;

import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.dim.base.BrowserDimension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import lombok.Data;

/**
 * 进行用户分析(用户基本分析和浏览器分析）定义的组合维度
 */
@Data
public class StatsUserDimension extends StatsDimension {

  private StatsCommonDimension statsCommon = new StatsCommonDimension();
  private BrowserDimension browser = new BrowserDimension();

  public StatsUserDimension() {
  }

  public StatsUserDimension(
      StatsCommonDimension statsCommon, BrowserDimension browser) {
    super();
    this.statsCommon = statsCommon;
    this.browser = browser;
  }


  public static StatsUserDimension clone(StatsUserDimension dimension) {
    BrowserDimension browser = new BrowserDimension(dimension.browser.getBrowserName(),
        dimension.browser.getBrowserVersion());
    StatsCommonDimension statsCommon = StatsCommonDimension.clone((dimension.statsCommon));
    return new StatsUserDimension(statsCommon, browser);
  }

  @Override
  public int compareTo(BaseDimension o) {
    if (this == o) {
      return 0;
    }
    StatsUserDimension other = (StatsUserDimension) o;
    int tmp = this.statsCommon.compareTo(other.statsCommon);
    if (tmp != 0) {
      return tmp;

    }

    tmp = this.browser.compareTo(other.browser);
    return tmp;
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    this.statsCommon.write(dataOutput);
    this.browser.write(dataOutput);
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.statsCommon.readFields(dataInput);
    this.browser.readFields(dataInput);
  }

}
