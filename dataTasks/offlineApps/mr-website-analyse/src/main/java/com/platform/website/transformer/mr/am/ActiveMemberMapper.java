package com.platform.website.transformer.mr.am;

import com.platform.website.common.DateEnum;
import com.platform.website.common.EventLogConstants;
import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.base.BrowserDimension;
import com.platform.website.transformer.model.dim.base.DateDimension;
import com.platform.website.transformer.model.dim.base.KpiDimension;
import com.platform.website.transformer.model.dim.base.PlatformDimension;
import com.platform.website.transformer.model.dim.StatsCommonDimension;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

public class ActiveMemberMapper extends TableMapper<StatsUserDimension, TimeOutputValue> {

  private static final Logger logger = Logger.getLogger(
      ActiveMemberMapper.class);
  private byte[] family = Bytes.toBytes(EventLogConstants.EVENT_LOGS_FAMILY_NAME);
  private StatsUserDimension outputKey = new StatsUserDimension();
  private TimeOutputValue outputValue = new TimeOutputValue();
  private BrowserDimension defaultBrowser = new BrowserDimension("", "");

  private KpiDimension activeMemberKpi = new KpiDimension(KpiType.ACTIVE_MEMBER.name);
  private KpiDimension activeMemberOfBrowserKpi = new KpiDimension(
      KpiType.BROWSER_ACTIVE_MEMBER.name);

  @Override
  protected void map(ImmutableBytesWritable key, Result value, Context context)
      throws IOException, InterruptedException {
    String memberId = Bytes
        .toString(value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_MEMBER_ID)));
    String platform = Bytes.toString(
        value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_PLATFORM)));
    String serverTime = Bytes.toString(
        value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME)));
    if (StringUtils.isBlank(memberId) || StringUtils.isBlank(serverTime) || StringUtils
        .isBlank(platform) || !StringUtils.isNumeric(serverTime.trim())) {
      System.out.println(Bytes.toString(value.getRow()));
      logger.warn("memberId&servertime&platform不能为空，而且serverTime必须为时间戳");
      return;
    }
    long longOfTime = Long.valueOf(serverTime.trim());
    if (longOfTime == -1) {
      //没有传s_time参数
      longOfTime = new Date().getTime();
    }
    DateDimension dateDimension = DateDimension.buildDate(longOfTime, DateEnum.DAY);
    outputValue.setId(memberId);
    List<PlatformDimension> platformDimensions = PlatformDimension.buildList(platform);
    //写browser相关的数据
    String browserName = Bytes.toString(
        value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_BROWSER_NAME)));
    String browserVersion = Bytes.toString(
        value.getValue(family, Bytes.toBytes(EventLogConstants.LOG_COLUMN_NAME_BROWSER_VERSION)));
    List<BrowserDimension> browserDimensions = BrowserDimension
        .buildList(browserName, browserVersion);

    //开始进行输出
    StatsCommonDimension statsCommonDimension = this.outputKey.getStatsCommon();
    statsCommonDimension.setDate(dateDimension);

    for (PlatformDimension pf : platformDimensions) {
      //清空BrowserDimenson的内容
      outputKey.setBrowser(defaultBrowser);
      //设置platform dimension
      statsCommonDimension.setPlatform(pf);
      //设置kpi dimension
      statsCommonDimension.setKpi(activeMemberKpi);
      context.write(this.outputKey, this.outputValue);
      for (BrowserDimension br : browserDimensions) {
        statsCommonDimension.setKpi(activeMemberOfBrowserKpi);
        this.outputKey.setBrowser(br);
        context.write(this.outputKey, this.outputValue);
      }
    }

  }

}
