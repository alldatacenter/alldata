package com.platform.schedule.task.online;

import com.platform.schedule.entity.RatingEntity;
import com.platform.schedule.entity.TopEntity;
import com.platform.schedule.function.RatingEntityMap;
import com.platform.schedule.function.AggCount;
import com.platform.schedule.function.HotProducts;
import com.platform.schedule.entity.Property;
import com.platform.schedule.function.WindowResultFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

public class TopNProductTask {

    private static final int topCount = 10;

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 基于EventTime
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        Properties properties = Property.getKafkaProperties("topProduct");
        DataStreamSource<String> datasource = env.addSource(
                new FlinkKafkaConsumer<String>("rating", new SimpleStringSchema(), properties)
        );
        DataStream<RatingEntity> source = datasource.map(new RatingEntityMap());
        // 抽取时间戳，生成 watermark
        DataStream<RatingEntity> timeData = source.assignTimestampsAndWatermarks(new AscendingTimestampExtractor<RatingEntity>() {
            @Override
            public long extractAscendingTimestamp(RatingEntity ratingEntity) {
                return ratingEntity.getTimestamp() * 1000;
            }
        });
        // 窗口统计点击量，数据集的原因，每一次评分当做一次点击
        DataStream<TopEntity> windowData = timeData.keyBy("productId")
                .timeWindow(Time.minutes(60), Time.minutes(1))
                .aggregate(new AggCount(), new WindowResultFunction());
        DataStream<String> topProducts = windowData.keyBy("windowEnd")
                .process(new HotProducts(topCount));
        //存入 hbase  表 "onlineHot", |rowkey||
        topProducts.print();
        env.execute("Hot Product Task");
    }
}
