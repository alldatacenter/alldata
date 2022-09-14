package com.webank.wedatasphere.streamis.project.server.utils;

import org.apache.commons.math3.util.Pair;
import org.apache.linkis.server.Message;

import java.util.Arrays;


public class StreamisProjectRestfulUtils {



    public static Message dealError(String reason){
        return Message.error(reason);
    }

    public static Message dealOk(String msg){
        return Message.ok(msg);
    }



    @SafeVarargs
    public static Message dealOk(String msg, Pair<String, Object>... data){
        Message message = Message.ok(msg);
        Arrays.stream(data).forEach(p -> message.data(p.getKey(), p.getValue()));
        return message;
    }


}
