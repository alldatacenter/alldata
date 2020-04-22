package com.platform.website.transformer.mr.sessions;

import com.platform.website.common.DateEnum;
import com.platform.website.common.GlobalConstants;
import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsUserDimension;
import com.platform.website.transformer.model.value.map.TimeOutputValue;
import com.platform.website.transformer.model.value.reduce.MapWritableValue;
import com.platform.website.util.TimeChain;
import com.platform.website.util.TimeUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * 计算new install user的reduce类
 */
public class SessionsReducer extends
    Reducer<StatsUserDimension, TimeOutputValue, StatsUserDimension, MapWritableValue> {

  private Map<Integer, Map<String, TimeChain>> hourlyTimeChainMap = new HashMap<>(); //用于统计hourly的会话个数和会话时长
  private MapWritableValue outputValue = new MapWritableValue();
  private Map<String, TimeChain> timeChainMap = new HashMap<>();
  private MapWritable map = new MapWritable();
  private MapWritable hourlySessionsMap = new MapWritable();
  private MapWritable hourlySessionsLengthMap = new MapWritable();

  private void startUp() {
    this.map.clear();
    this.timeChainMap.clear();
    this.hourlySessionsMap.clear();
    this.hourlySessionsLengthMap.clear();
    this.hourlyTimeChainMap.clear();
    for (int i = 0; i < 24; i++) {
      this.hourlySessionsMap.put(new IntWritable(i), new IntWritable(0));
      this.hourlySessionsLengthMap.put(new IntWritable(i), new IntWritable(0));
      this.hourlyTimeChainMap.put(i, new HashMap<String, TimeChain>());
    }
  }


  @Override
  protected void reduce(StatsUserDimension key, Iterable<TimeOutputValue> values, Context context)
      throws IOException, InterruptedException {
    this.startUp();
    String kpiName = key.getStatsCommon().getKpi().getKpiName();
    if (KpiType.SESSIONS.name.equals(kpiName)) {
      //计算stats_user表的sessions和sessions_length，同时也计算hourly_sessions&sessions_length
      this.handleSessions(key, values, context);
    } else if (KpiType.BROWSER_SESSIONS.equals(kpiName)) {
      //处理browser维度的统计信息
      this.handleBrowserSessions(key, values, context);
    }


  }

  /**
   * 处理普通的sessions分析
   */
  private void handleSessions(StatsUserDimension key, Iterable<TimeOutputValue> values,
      Context context)
      throws IOException, InterruptedException {

    //开始计算memberId的个数
    for (TimeOutputValue value : values) {
      String sid = value.getId();
      long time = value.getTime();

      //处理正常统计
      TimeChain chain = this.timeChainMap.get(sid);
      if (chain == null) {
        chain = new TimeChain(time);
        this.timeChainMap.put(sid, chain);//保存
      }
      chain.addTime(time);

      //处理hourly统计
      int hour = TimeUtil.getDateInfo(time, DateEnum.HOUR);
      Map<String, TimeChain> htcm = this.hourlyTimeChainMap.get(hour);
      TimeChain hourlyChain = htcm.get(sid);
      if (hourlyChain == null) {
        hourlyChain = new TimeChain(time);
        htcm.put(sid, hourlyChain);
        this.hourlyTimeChainMap.put(hour, htcm);
      }
      hourlyChain.addTime(time);
    }

    //计算hourly统计信息
    for (Map.Entry<Integer, Map<String, TimeChain>> entry : this.hourlyTimeChainMap.entrySet()) {
      this.hourlySessionsMap.put(new IntWritable(entry.getKey()),
          new IntWritable(entry.getValue().size())); //设置当前小时的session个数
      int presl = 0; //统计每小时的会话时长
      for (Map.Entry<String, TimeChain> entry2 : entry.getValue().entrySet()) {
        long tmp = entry2.getValue().getTimeOfMillis();//间隔毫秒数
        if (tmp < 0 || tmp > 3600000) {
          //会话小于0或者大于1小时
          continue;
        }
        presl += tmp;
      }

      //2计算间隔秒数
      if (presl % 1000 == 0) {
        presl = presl / 1000;
      } else {
        presl = presl / 1000 + 1;
      }
      this.hourlySessionsLengthMap.put(new IntWritable(entry.getKey()), new IntWritable(presl));
    }


    //进行hourly输出
    outputValue.setValue(this.hourlySessionsMap);
    //设置kpi
    outputValue.setKpi(KpiType.HOURLY_SESSIONS);
    context.write(key, outputValue);

    //进行hourly输出
    outputValue.setValue(this.hourlySessionsLengthMap);
    //设置kpi
    outputValue.setKpi(KpiType.HOURLY_SESSIONS_LENGTH);
    context.write(key, outputValue);

        //计算间隔秒数
    int sessionsLength = 0;
    // 1计算间隔毫秒数
    for (Map.Entry<String, TimeChain> entry : this.timeChainMap.entrySet()) {
      long tmp = entry.getValue().getTimeOfMillis();
      if (tmp < 0 || tmp > GlobalConstants.DAY_OF_MILLISECONDS) {
        continue;//如果计算的值小于0 或者大于一天的毫秒数,直接过滤
      }
      sessionsLength += tmp;
    }
    //2计算间隔秒数
    if (sessionsLength % 1000 == 0) {
      sessionsLength = sessionsLength / 1000;
    } else {
      sessionsLength = sessionsLength / 1000 + 1;
    }

    //设置value
    this.map.put(new IntWritable(-1), new IntWritable(this.timeChainMap.size()));
    this.map.put(new IntWritable(-2), new IntWritable(sessionsLength));
    outputValue.setValue(this.map);
    //设置kpi
    outputValue.setKpi(KpiType.valueOfName(key.getStatsCommon().getKpi().getKpiName()));
    context.write(key, outputValue);
  }

  private void handleBrowserSessions(StatsUserDimension key, Iterable<TimeOutputValue> values,
      Context context)
      throws IOException, InterruptedException {
    //开始计算memberId的个数
    for (TimeOutputValue value : values) {
//        this.unique.add(value.getId());
      TimeChain chain = this.timeChainMap.get(value.getId());
      if (chain == null) {
        chain = new TimeChain(value.getTime())
        ;
        this.timeChainMap.put(value.getId(), chain);//保存
      }
      chain.addTime(value.getTime());
    }
    //计算间隔秒数
    int sessionsLength = 0;
    // 1计算间隔毫秒数
    for (Map.Entry<String, TimeChain> entry : this.timeChainMap.entrySet()) {
      long tmp = entry.getValue().getTimeOfMillis();
      if (tmp < 0 || tmp > GlobalConstants.DAY_OF_MILLISECONDS) {
        continue;//如果计算的值小于0 或者大于一天的毫秒数,直接过滤
      }
      sessionsLength += tmp;
    }
    //2计算间隔秒数
    if (sessionsLength % 1000 == 0) {
      sessionsLength = sessionsLength / 1000;
    } else {
      sessionsLength = sessionsLength / 1000 + 1;
    }

    //设置value
    this.map.put(new IntWritable(-1), new IntWritable(this.timeChainMap.size()));
    this.map.put(new IntWritable(-2), new IntWritable(sessionsLength));
    outputValue.setValue(this.map);

    //设置kpi
    outputValue.setKpi(KpiType.BROWSER_SESSIONS);
    context.write(key, outputValue);
  }


}
