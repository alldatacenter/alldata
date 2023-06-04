package com.platform.flink;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * websocket客户端
 */
@ClientEndpoint
@Slf4j
public class WebSocketClientUtils {

    //会话
    private Session session;
    private URI uri;

    //构造函数
    public WebSocketClientUtils(String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            log.error("Websocket的地址错误", e);
        }
    }

    @OnOpen
    public void open(Session session) {
        log.info("Websocket正在开启会话");
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("Websocket接收信息:" + message);
    }

    @OnClose
    public void onClose() {
        log.info("Websocket已关闭");
    }

    @OnError
    public void onError(Session session, Throwable t) {
        log.error("Websocket报错", t);
    }

    //初始化
    public void init() {
        WebSocketContainer conmtainer = ContainerProvider.getWebSocketContainer();
        try {
            conmtainer.connectToServer(this, uri);
        } catch (Exception e) {
            log.error("Websocket创建报错");
        }
    }

    //发送字符串
    public void send(String message) {
        if (session == null || !session.isOpen()) {
            log.error("Websocket状态异常,不能发送信息 - 尝试稍后连接");
            init();
        } else {
            this.session.getAsyncRemote().sendText(message);
        }
    }

    //关闭连接
    public void close() {
        if (this.session.isOpen()) {
            try {
                this.session.close();
            } catch (IOException e) {
                log.error("Websocket关闭报错", e);
            }
        }
    }

    //测试
    public static void main(String[] args) throws Exception {
        WebSocketClientUtils client = new WebSocketClientUtils("ws://localhost:5001/websocket/server");
        client.init();
        int turn = 0;
        while (turn++ < 1000) {
            HashMap<String, Object> websocketMessage = new HashMap<>();
            websocketMessage.put("toGroupIds", new String[]{"html"});
            websocketMessage.put("toClientIds", new String[]{});
            websocketMessage.put("channel", "test_data_alert");
            websocketMessage.put("message", "23123123");
            client.send(JSON.toJSONString(websocketMessage));
            log.info(String.valueOf(turn));
            Thread.sleep(1000);
        }
        client.close();
    }

}
