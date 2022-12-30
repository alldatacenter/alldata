package com.platform.website.transformer.mr.location;

import com.platform.website.common.DateEnum;
import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsLocationDimension;
import com.platform.website.transformer.model.dim.base.DateDimension;
import com.platform.website.transformer.model.dim.base.KpiDimension;
import com.platform.website.transformer.model.dim.base.LocationDimension;
import com.platform.website.transformer.model.dim.base.PlatformDimension;
import com.platform.website.transformer.model.dim.StatsCommonDimension;
import com.platform.website.transformer.model.value.map.TextsOutputValue;
import com.platform.website.transformer.mr.TransformerBaseMapper;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

/**
 * 统计location纬度信息的mapper类
 */
public class LocationMapper extends TransformerBaseMapper<StatsLocationDimension, TextsOutputValue> {

  private static final Logger logger = Logger.getLogger(
      LocationMapper.class);
  private StatsLocationDimension statsLocationDimension = new StatsLocationDimension();
  private TextsOutputValue outputValue = new TextsOutputValue();

  private KpiDimension locationKpiDimension = new KpiDimension(KpiType.LOCATION.name);

  @Override
  protected void map(ImmutableBytesWritable key, Result value, Context context)
      throws IOException, InterruptedException {
    this.inputRecords++;
    String platform = this.getPlatform(value);
    String serverTime = this.getServerTime(value);
    String uuid = this.getUuid(value);
    String sid =  this.getSessionId(value);

    if (StringUtils.isBlank(platform) || StringUtils.isBlank(uuid) || StringUtils
        .isBlank(serverTime) || StringUtils.isBlank(sid) || !StringUtils
        .isNumeric(serverTime.trim())) {
      System.out.println(Bytes.toString(value.getRow()));
      logger.warn("平台&uuid&会话id&servertime不能为空，而且serverTime必须为时间戳");
      this.filterRecords++;
      return;
    }

    long longOfTime = Long.valueOf(serverTime.trim());
    if (longOfTime == -1) {
      //没有传s_time参数
      longOfTime = new Date().getTime();
    }

    //时间纬度创建
    DateDimension dateDimension = DateDimension.buildDate(longOfTime, DateEnum.DAY);

    //平台维度创建
    List<PlatformDimension> platformDimensions = PlatformDimension.buildList(platform);

    //location维度创建
    String country = this.getCountry(value);
    String province = this.getProvince(value);
    String city = this.getCity(value);

    List<LocationDimension> locationDimensions = LocationDimension
        .buildList(country, province, city);

    //进行输出定义
    this.outputValue.setUuid(uuid);
    this.outputValue.setSid(sid);
    StatsCommonDimension statsCommonDimension = this.statsLocationDimension.getStatsCommon();
    statsCommonDimension.setDate(dateDimension);
    statsCommonDimension.setKpi(this.locationKpiDimension);
    for (PlatformDimension pf : platformDimensions) {
      //设置platform dimension
      statsCommonDimension.setPlatform(pf);
      for (LocationDimension location : locationDimensions) {
        this.statsLocationDimension.setLocation(location);
        context.write(this.statsLocationDimension, this.outputValue);
        this.outputRecords++;
      }
    }

  }

}
