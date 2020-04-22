package com.platform.website.transformer.mr.au;

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
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.log4j.Logger;

public class ActiveUserMapper extends TransformerBaseMapper<StatsUserDimension, TimeOutputValue> {
  private static final Logger logger = Logger.getLogger(ActiveUserMapper.class);
  private StatsUserDimension outputKey = new StatsUserDimension();
  private TimeOutputValue outputValue = new TimeOutputValue();
  private BrowserDimension defaultBrowser = new BrowserDimension("", ""); // 默认的browser对象
  private KpiDimension activeUserKpi = new KpiDimension(KpiType.ACTIVE_USER.name);
  private KpiDimension activeUserOfBrowserKpi = new KpiDimension(KpiType.BROWSER_ACTIVE_USER.name);
  private KpiDimension hourlyActiveUserKpi = new KpiDimension(KpiType.HOURLY_ACTIVE_USER.name);

  private String uuid,platform,serverTime,browser,browserVersion;

  @Override
  protected void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {
    this.inputRecords++;

    // 获取uuid&platform&serverTime，从hbase返回的结果集Result中
    this.uuid = this.getUuid(value);
    this.platform = this.getPlatform(value);
    this.serverTime = this.getServerTime(value);

    // 过滤无效数据
    if (StringUtils.isBlank(uuid) || StringUtils.isBlank(platform) || StringUtils.isBlank(serverTime) || !StringUtils.isNumeric(serverTime.trim())) {
      logger.warn("uuid&platform&serverTime不能为空，而且serverTime必须为时间戳");
      this.filterRecords++;
      return;
    }

    long longOfServerTime = Long.valueOf(serverTime.trim());
    DateDimension dateDimension = DateDimension.buildDate(longOfServerTime, DateEnum.DAY);
    this.outputValue.setId(uuid); // 设置用户id
    this.outputValue.setTime(longOfServerTime); // 设置访问的服务器时间，可以用来计算该用户访问的时间是哪个时间段。

    // 进行platform的构建
    List<PlatformDimension> platforms = PlatformDimension.buildList(platform); // 进行platform创建
    // 获取browser name和browser version
    this.browser = this.getBrowserName(value);
    this.browserVersion = this.getBrowserVersion(value);
    // 进行browser的维度信息构建
    List<BrowserDimension> browsers = BrowserDimension.buildList(browser, browserVersion);

    // 开始进行输出
    StatsCommonDimension statsCommonDimension = this.outputKey.getStatsCommon();
    // 设置date dimension
    statsCommonDimension.setDate(dateDimension);
    for (PlatformDimension pf : platforms) {
      this.outputKey.setBrowser(defaultBrowser); // 进行覆盖操作
      // 设置platform dimension
      statsCommonDimension.setPlatform(pf);

      // 输出active user的键值对
      // 设置kpi dimension
      statsCommonDimension.setKpi(activeUserKpi);
      context.write(this.outputKey, this.outputValue);
      this.outputRecords++;

      // 输出hourly active user的键值对
      statsCommonDimension.setKpi(this.hourlyActiveUserKpi);
      context.write(this.outputKey, this.outputValue);
      this.outputRecords++;

      // 输出browser维度统计
      statsCommonDimension.setKpi(activeUserOfBrowserKpi);
      for (BrowserDimension bw : browsers) {
        this.outputKey.setBrowser(bw); // 设置对应的browsers
        context.write(this.outputKey, this.outputValue);
        this.outputRecords++;
      }
    }
  }
}
