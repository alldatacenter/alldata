package com.alibaba.sreworks.job.master.event;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class KafkaJobEventConsumer extends AbstractJobEventConsumer {

//    private String kafkaEndpoint = "sreworks-kafka.sreworks:9092";

    public JobEventConf conf;

    public KafkaConsumer<String, String> consumer;

    public KafkaJobEventConsumer(JobEventConf conf) {
        this.conf = conf;
        Properties props = new Properties();
        props.put("bootstrap.servers", conf.getConfig().getString("server"));
//        props.put("group.id", conf.getConfig().getString("groupId"));
        props.put("group.id", "group-" + UUID.randomUUID());
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(conf.getConfig().getJSONArray("topics").toJavaList(String.class));
    }

    public List<JSONObject> poll() {
        List<JSONObject> ret = new ArrayList<>();
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
            ret.add(JSONObject.parseObject(record.value()));
        }
        return ret;
    }

    public void close() {
        consumer.close();
    }

}
