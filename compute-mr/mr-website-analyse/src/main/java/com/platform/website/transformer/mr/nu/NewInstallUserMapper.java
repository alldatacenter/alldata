package com.platform.website.transformer.mr.nu;

import com.platform.website.common.DateEnum;
import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.base.BrowserDimension;
import com.platform.website.transformer.model.dim.base.DateDimension;
import com.platform.website.transformer.model.dim.base.KpiDimension;
import com.platform.website.transformer.model.dim.base.PlatformDimension;
import com.platform.website.transformer.model.dim.StatsCommonDimension;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.mr.TransformerBaseMapper;
import java.io.IOException;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.log4j.Logger;

public class NewInstallUserMapper extends TransformerBaseMapper<StatsUserDimension, TimeOutputValue> {
  private static final Logger logger = Logger.getLogger(NewInstallUserMapper.class);
  private StatsUserDimension statsUserDimension = new StatsUserDimension();
  private TimeOutputValue timeOutputValue = new TimeOutputValue();
  private KpiDimension newInstallUserKpi = new KpiDimension(KpiType.NEW_INSTALL_USER.name);
  private KpiDimension newInstallUserOfBrowserKpi = new KpiDimension(KpiType.BROWSER_NEW_INSTALL_USER.name);

  @Override
  protected void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {
    this.inputRecords++;
    String uuid = super.getUuid(value);
    String serverTime = super.getServerTime(value);
    String platform = super.getPlatform(value);
    if (StringUtils.isBlank(uuid) || StringUtils.isBlank(serverTime) || StringUtils.isBlank(platform)) {
      this.filterRecords++;
      logger.warn("uuid&servertime&platform不能为空");
      return;
    }
    long longOfTime = Long.valueOf(serverTime.trim());
    timeOutputValue.setId(uuid); // 设置id为uuid
    timeOutputValue.setTime(longOfTime); // 设置时间为服务器时间
    DateDimension dateDimension = DateDimension.buildDate(longOfTime, DateEnum.DAY);
    List<PlatformDimension> platformDimensions = PlatformDimension.buildList(platform);

    // 设置date维度
    StatsCommonDimension statsCommonDimension = this.statsUserDimension.getStatsCommon();
    statsCommonDimension.setDate(dateDimension);
    // 写browser相关的数据
    String browserName = super.getBrowserName(value);
    String browserVersion = super.getBrowserVersion(value);
    List<BrowserDimension> browserDimensions = BrowserDimension.buildList(browserName, browserVersion);
    BrowserDimension defaultBrowser = new BrowserDimension("", "");
    for (PlatformDimension pf : platformDimensions) {
      // 1. 设置为一个默认值
      statsUserDimension.setBrowser(defaultBrowser);
      // 2. 解决有空的browser输出的bug
      // statsUserDimension.getBrowser().clean();
      statsCommonDimension.setKpi(newInstallUserKpi);
      statsCommonDimension.setPlatform(pf);
      context.write(statsUserDimension, timeOutputValue);
      this.outputRecords++;
      for (BrowserDimension br : browserDimensions) {
        statsCommonDimension.setKpi(newInstallUserOfBrowserKpi);
        // 1.
        statsUserDimension.setBrowser(br);
        // 2. 由于上面需要进行clean操作，故将该值进行clone后填充
        // statsUserDimension.setBrowser(WritableUtils.clone(br,
        // context.getConfiguration()));
        context.write(statsUserDimension, timeOutputValue);
        this.outputRecords++;
      }
    }
  }
}
