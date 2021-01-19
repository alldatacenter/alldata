package com.platform.task.task.dataloader;

import com.platform.task.entity.RatingEntity;
import com.platform.task.function.DataToHbaseMapFunction;
import com.platform.task.util.Property;
import org.apache.flink.api.java.io.CsvInputFormat;
import org.apache.flink.api.java.io.PojoCsvInputFormat;
import org.apache.flink.api.java.typeutils.PojoTypeInfo;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class DataOnboard {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        String ratingFilePath = Property.getStrValue("rating.data.path");
        PojoTypeInfo<RatingEntity> typeInfo = (PojoTypeInfo<RatingEntity>) TypeExtractor.createTypeInfo(RatingEntity.class);
        String[] fields = new String[]{"userId", "productId", "score", "timestamp"};
        CsvInputFormat<RatingEntity> csvInputFormat = new PojoCsvInputFormat(new Path(ratingFilePath), typeInfo, fields);
        csvInputFormat.setFieldDelimiter(",");
        DataStream<RatingEntity> productDataStream = env.createInput(csvInputFormat, typeInfo);
        productDataStream.map(new DataToHbaseMapFunction()).print();
        env.execute("Load Rating Data");
    }
}
