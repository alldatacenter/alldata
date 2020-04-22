package com.platform.website.transformer.mr.nm;

import com.platform.website.common.DateEnum;
import com.platform.website.common.GlobalConstants;
import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.base.BrowserDimension;
import com.platform.website.transformer.model.dim.base.DateDimension;
import com.platform.website.transformer.model.dim.base.KpiDimension;
import com.platform.website.transformer.model.dim.base.PlatformDimension;
import com.platform.website.transformer.model.dim.StatsCommonDimension;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.mr.TransformerBaseMapper;
import com.platform.website.util.JdbcManager;
import com.platform.website.util.MemberUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.log4j.Logger;

public class NewMemberMapper extends TransformerBaseMapper<StatsUserDimension, TimeOutputValue> {
  private static final Logger logger = Logger.getLogger(NewMemberMapper.class);
  private StatsUserDimension outputKey = new StatsUserDimension();
  private TimeOutputValue outputValue = new TimeOutputValue();
  private KpiDimension newMemberKpi = new KpiDimension(KpiType.NEW_MEMBER.name);
  private KpiDimension newMemberOfBrowserKpi = new KpiDimension(KpiType.BROWSER_NEW_MEMBER.name);
  private BrowserDimension defaultBrowserDimension = new BrowserDimension("", "");
  private Connection connection = null;

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    super.setup(context);
    // 进行初始化操作
    Configuration conf = context.getConfiguration();
    try {
      this.connection = JdbcManager.getConnection(conf, GlobalConstants.WAREHOUSE_OF_WEBSITE);
      // 删除指定日期的数据
      MemberUtil.deleteMemberInfoByDate(conf.get(GlobalConstants.RUNNING_DATE_PARAMS), this.connection);
    } catch (SQLException e) {
      logger.error("获取数据库连接出现异常", e);
      throw new IOException("数据库连接信息获取失败", e);
    }
  }

  @Override
  protected void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {
    // 获取会员id
    String memberId = this.getMemberId(value);
    // 判断member id是否是第一次访问
    try {
      if (StringUtils.isBlank(memberId) || !MemberUtil.isValidateMemberId(memberId) || !MemberUtil.isNewMemberId(memberId, this.connection)) {
        logger.warn("member id不能为空，而且要是是第一次访问网站的会员id");
        return;
      }
    } catch (SQLException e) {
      logger.error("查询会员id是否是新会员id出现数据库异常", e);
      throw new IOException("查询数据库出现异常", e);
    }

    // member id是第一次访问，获取平台名称、服务器时间
    String platform = this.getPlatform(value);
    String serverTime = this.getServerTime(value);

    // 过滤无效数据
    if (StringUtils.isBlank(platform) || StringUtils.isBlank(serverTime) || !StringUtils.isNumeric(serverTime.trim())) {
      logger.warn("平台名称&服务器时间不能为空，而且服务器时间必须为时间戳形式");
      return;
    }

    long longOfTime = Long.valueOf(serverTime.trim());
    DateDimension dayOfDimension = DateDimension.buildDate(longOfTime, DateEnum.DAY);

    // 创建platform 维度信息
    List<PlatformDimension> platforms = PlatformDimension.buildList(platform);
    // 创建browser 维度信息
    String browserName = this.getBrowserName(value);
    String browserVersion = this.getBrowserVersion(value);
    List<BrowserDimension> browsers = BrowserDimension.buildList(browserName, browserVersion);
    // 设置输出
    this.outputValue.setId(memberId);
    StatsCommonDimension statsCommon = this.outputKey.getStatsCommon();
    statsCommon.setDate(dayOfDimension);
    for (PlatformDimension pf : platforms) {
      // 基本信息输出
      this.outputKey.setBrowser(this.defaultBrowserDimension); // 设置一个默认值，方便进行控制
      statsCommon.setKpi(this.newMemberKpi);
      statsCommon.setPlatform(pf);
      context.write(this.outputKey, this.outputValue);

      // 浏览器信息输出
      statsCommon.setKpi(this.newMemberOfBrowserKpi);
      for (BrowserDimension br : browsers) {
        this.outputKey.setBrowser(br);
        context.write(this.outputKey, this.outputValue);
      }
    }

  }

  @Override
  protected void cleanup(Context context) throws IOException, InterruptedException {
    try {
      super.cleanup(context);
    } finally {
      // 关闭数据库连接
      if (this.connection != null) {
        try {
          this.connection.close();
        } catch (SQLException e) {
          // nothing
        }
      }
    }
  }
}
