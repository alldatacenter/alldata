package com.platform.flink;

import com.alibaba.fastjson.JSON;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.util.HashMap;
import java.util.Map;

public class WebsocketSink extends RichSinkFunction<Tuple4<Long, Long, Long, Integer>> {

    //私有
    private static WebSocketClientUtils webSocketClient;
    private String wsUrl;

    //构造函数
    public WebsocketSink(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        //websocket客户端
        webSocketClient = new WebSocketClientUtils(wsUrl);
        webSocketClient.init();
    }

    @Override
    public void invoke(Tuple4<Long, Long, Long, Integer> result, Context context) throws Exception {
        //websocket数据发送
        if (result != null) {
            Map<String, Object> output = new HashMap<>();
            output.put("window_start", result.f0);
            output.put("window_end", result.f1);
            output.put("pv", result.f2);
            output.put("uv", result.f3);
            webSocketClient.send(JSON.toJSONString(output));
        }
    }
}

