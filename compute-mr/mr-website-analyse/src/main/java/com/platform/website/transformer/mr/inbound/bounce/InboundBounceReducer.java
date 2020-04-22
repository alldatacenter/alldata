package com.platform.website.transformer.mr.inbound.bounce;

import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.StatsCommonDimension;
import com.platform.website.transformer.model.dim.StatsInboundDimension;
import com.platform.website.transformer.model.dim.base.StatsInboundBounceDimension;
import com.platform.website.transformer.model.value.reduce.InboundBounceReduceValue;
import com.platform.website.transformer.model.value.reduce.InboundReduceValue;
import com.platform.website.transformer.service.impl.InboundDimensionService;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * 计算外链跳出会话的reduce类
 */
public class InboundBounceReducer extends
    Reducer<StatsInboundBounceDimension, IntWritable, StatsInboundDimension, InboundBounceReduceValue> {
  private StatsInboundDimension statsInboundDimension = new StatsInboundDimension();
  private Set<String> uvs = new HashSet<String>();
  private Set<String> visits = new HashSet<String>();
  private InboundReduceValue outputValue = new InboundReduceValue();

  @Override
  protected void reduce(StatsInboundBounceDimension key, Iterable<IntWritable> values,
      Context context)
      throws IOException, InterruptedException {

    String preSid = "";//前一条记录的会话id
    String curSid = ""; //当前会话id
    int curInboundId = InboundBounceMapper.DEFAULT_INBOUND_ID; //当前inbound id
    int preInboundId = InboundBounceMapper.DEFAULT_INBOUND_ID; //前一个记录的inbound id
    boolean isBounceVisit = true;//true表示会话是一个跳出会话，否则不是跳出会话
     boolean isNewSession = false; //表示一个新的会话

    Map<Integer, InboundBounceReduceValue> map = new HashMap<>();
    map.put(InboundDimensionService.ALL_OF_INBOUND_ID, new InboundBounceReduceValue());
    for (IntWritable value : values) {
      curSid = key.getSid();
      curInboundId = value.get();

      //同一个会话而且当前的inboundid为0，那么一定是一个非跳出的visit
      if (curSid.equals(preSid) && (curInboundId == InboundBounceMapper.DEFAULT_INBOUND_ID
          || curInboundId == preInboundId)) {
        isBounceVisit = false;
        continue;
      }

      //跳出外链 总的两种情况：
      // 1. 一个新的会话
      // 2. 一个非0的 inbound id

      //表示是一个新会话或者是一个新的inbound， 检查上一个inbound是否是一个跳出的inbound
      if (preInboundId != InboundBounceMapper.DEFAULT_INBOUND_ID && isBounceVisit) {
        //针对上一个inbound id 需要进行一个跳出会话的更新操作
        map.get(preInboundId).incrBounceNum();

        //针对all维度的bounce number计算
        // 规则1： 一次会话中只要出现一次跳出外链，就将其算到all维度的跳出会话中去，只要出现一次跳出就算做跳出
        if (!curSid.equals(preSid)){
          map.get(InboundDimensionService.ALL_OF_INBOUND_ID).incrBounceNum();
        }
      }

      //会话结束或者当前inbound id不为0，而且和前一个inboundid不相等
      isBounceVisit = true;
      preInboundId = InboundBounceMapper.DEFAULT_INBOUND_ID;

      //如果inbound是一个新的
      if (curInboundId != InboundBounceMapper.DEFAULT_INBOUND_ID) {
        preInboundId = curInboundId;
        InboundBounceReduceValue irv = map.get(curInboundId);
        if (irv == null){
          irv = new InboundBounceReduceValue(0);
          map.put(curInboundId, irv);
        }
      }

      //如果是一个新的会话，那么更新会话
      if (!preSid.equals(curSid)) {
        isNewSession = true;
        preSid = curSid;
      }else {
        isNewSession = false;
      }
    }

    //单独处理最后一条数据
    //表示是一个新会话或者是一个新的inbound， 检查上一个inbound是否是一个跳出的inbound
    if (preInboundId != InboundBounceMapper.DEFAULT_INBOUND_ID && isBounceVisit) {
      //针对上一个inbound id 需要进行一个跳出会话的更新操作
      map.get(preInboundId).incrBounceNum();

      //针对all维度的bounce number计算
      // 规则1： 一次会话中只要出现一次跳出外链，就将其算到all维度的跳出会话中去，只要出现一次跳出就算做跳出
      if (isNewSession){
        map.get(InboundDimensionService.ALL_OF_INBOUND_ID).incrBounceNum();
      }
    }

    //数据输出
    this.statsInboundDimension.setStatsCommon(StatsCommonDimension.clone(key.getStatsCommon()));
    for(Map.Entry<Integer, InboundBounceReduceValue> entry: map.entrySet()){
      InboundBounceReduceValue value = entry.getValue();
      value.setKpi(KpiType.valueOfName(key.getStatsCommon().getKpi().getKpiName()));
      this.statsInboundDimension.getInbound().setId(entry.getKey());
      context.write(this.statsInboundDimension, value);
    }
  }


}
