package com.platform.schedule.function;

import com.platform.schedule.entity.HbaseClient;
import com.platform.schedule.entity.TopEntity;
import com.sun.jmx.snmp.Timestamp;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HotProducts extends KeyedProcessFunction<Tuple, TopEntity, String> {
    private int topSize=3;

    public HotProducts(int topSize) {
        this.topSize = topSize;
    }
    private ListState<TopEntity> listState;

    public HotProducts() {
        super();
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        ListStateDescriptor<TopEntity> listStateDescriptor = new ListStateDescriptor<TopEntity>(
                "listState",
                TopEntity.class
                );
        listState = getRuntimeContext().getListState(listStateDescriptor);
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
        super.onTimer(timestamp, ctx, out);
        List<TopEntity> allProduct = new ArrayList<>();
        for(TopEntity topEntity : listState.get()) {
            allProduct.add(topEntity);
        }
        listState.clear();
        allProduct.sort(new Comparator<TopEntity>() {
            @Override
            public int compare(TopEntity o1, TopEntity o2) {
                return (int) (o2.getActionTimes() - o1.getActionTimes());
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("===============\n");
        sb.append("时间:\t").append(new Timestamp(timestamp-1)).append("\n");
       try {
           for(int i = 0; i < topSize && i < allProduct.size(); i++) {
               TopEntity topEntity = allProduct.get(i);
               sb.append("No").append(i).append(":")
                       .append("商品ID=").append(topEntity.getProductId())
                       .append(" 点击量").append(topEntity.getActionTimes())
                       .append("\n");
               HbaseClient.putData("onlineHot", String.valueOf(i), "p"
                       , "productId", String.valueOf(topEntity.getProductId()) );
               HbaseClient.putData("onlineHot", String.valueOf(i), "p"
                       , "count", String.valueOf(topEntity.getActionTimes()));
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
        sb.append("===============\n");
        out.collect(sb.toString());
    }

    @Override
    public void processElement(TopEntity topEntity, Context context, Collector<String> collector)
            throws Exception {
        listState.add(topEntity);
        context.timerService().registerEventTimeTimer(topEntity.getWindowEnd()+1);
    }
}
