package com.alibaba.sreworks.health.producer;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;


/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/03 15:51
 */
@Slf4j
public class CustomSerializer implements Serializer {
    @Override
    public byte[] serialize(String topic, Object data) {
        try {
            if (data == null){
                log.warn("Null received at serializing");
                return null;
            }
            return JSONObject.toJSONBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error when serializing Custom Object to byte[]");
        }
    }
}