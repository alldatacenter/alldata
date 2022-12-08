package com.platform.website.transformer.mr.inbound.bounce;

import com.platform.website.common.DateEnum;
import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsCommonDimension;
import com.platform.website.transformer.model.dim.base.DateDimension;
import com.platform.website.transformer.model.dim.base.KpiDimension;
import com.platform.website.transformer.model.dim.base.PlatformDimension;
import com.platform.website.transformer.model.dim.base.StatsInboundBounceDimension;
import com.platform.website.transformer.mr.TransformerBaseMapper;
import com.platform.website.transformer.mr.inbound.InboundMapper;
import com.platform.website.transformer.service.impl.InboundDimensionService;
import com.platform.website.util.UrlUtil;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.log4j.Logger;

public class InboundBounceMapper extends
    TransformerBaseMapper<StatsInboundBounceDimension, IntWritable> {

  private static final Logger logger = Logger.getLogger(
      InboundMapper.class);
  /**
   * 默认inbound id，用于标识补上外链
   */
  public  static final int DEFAULT_INBOUND_ID = 0;
  private Map<String, Integer> inbounds = null;
  private StatsInboundBounceDimension outputKey = new StatsInboundBounceDimension();
  private IntWritable outputValue = new IntWritable();
  private KpiDimension inboundBounceKpi = new KpiDimension(KpiType.INBOUND_BOUNCE.name);

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    super.setup(context);
    //获取inbound的相关数据
    try {
      this.inbounds = InboundDimensionService.getInboundByType(conf, 0);
    } catch (Exception e) {
      logger.error("获取外链id出现数据库异常", e);
      throw new IOException("出现异常", e);
    }

  }

  @Override
  protected void map(ImmutableBytesWritable key, Result value, Context context)
      throws IOException, InterruptedException {
    this.inputRecords++;
    String platform = this.getPlatform(value);
    String serverTime = this.getServerTime(value);
    String referrerUrl = this.getReferrerUrl(value);
    String sid = this.getSessionId(value);

    if (StringUtils.isBlank(platform) || StringUtils
        .isBlank(serverTime) || StringUtils
        .isBlank(referrerUrl) || StringUtils.isBlank(sid) || !StringUtils
        .isNumeric(serverTime.trim())) {
      System.out.println(Bytes.toString(value.getRow()));
      logger.warn("平台&前一个页面的会话id&servertime&referrerUrl不能为空，而且serverTime必须为时间戳");
      this.filterRecords++;
      return;
    }

    //构建inbound; 转换url为外链id
    int inboundId = 0;
    try {
      inboundId = this.getInboundIdByHost(UrlUtil.getHost(referrerUrl));
    } catch (Throwable e) {
      logger.warn("获取referrer url对应的inbound id异常: " + referrerUrl);
      inboundId = 0;
    }

    long longOfTime = Long.valueOf(serverTime.trim());
    if (longOfTime == -1) {
      //没有传s_time参数
      longOfTime = new Date().getTime();
    }

    //时间纬度创建
    DateDimension dateDimension = DateDimension.buildDate(longOfTime, DateEnum.DAY);
//
    //平台维度创建
    List<PlatformDimension> platformDimensions = PlatformDimension.buildList(platform);

    //进行输出定义
    this.outputValue.set(inboundId);
    StatsCommonDimension statsCommonDimension = this.outputKey.getStatsCommon();
    statsCommonDimension.setDate(dateDimension);
    statsCommonDimension.setKpi(this.inboundBounceKpi);

    this.outputKey.setSid(sid);
    this.outputKey.setServerTime(longOfTime);

    //输出
    for (PlatformDimension pf : platformDimensions) {
      //设置platform dimension
      statsCommonDimension.setPlatform(pf);
      context.write(this.outputKey, this.outputValue);
      this.outputRecords++;
    }

  }

  /**
   * 根据url的host来获取不同inbound id值,如果是统计网站本身的host,返回0，如果不是返回-1
   */
  private int getInboundIdByHost(String host) {
    int id = 0;
    if (UrlUtil.isValidateInboundHost(host)) {
      //有效host，获取inbound id
      id = InboundDimensionService.OTHER_OF_INBOUND_ID;

      //查看是否是一个具体的inbound id值
      for (Map.Entry<String, Integer> entry : this.inbounds.entrySet()) {
        String urlRegex = entry.getKey();
        if (host.equals(urlRegex) || host.startsWith(urlRegex) || host.matches(urlRegex)) {
          id = entry.getValue();
          break;
        }
      }
    }
    return id;
  }

}
