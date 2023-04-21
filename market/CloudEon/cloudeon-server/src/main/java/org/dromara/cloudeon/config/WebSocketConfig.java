package org.dromara.cloudeon.config;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.cloudeon.websocket.ChatWebSocketHandler;
import org.dromara.cloudeon.websocket.LogWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
 
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        LogWebSocketHandler logWebSocketHandler = SpringUtil.getBean(LogWebSocketHandler.class);
        registry.addHandler(new ChatWebSocketHandler(), "/chat");
        // websocket一定要加上 * 匹配，否则前端连不上
        registry.addHandler(logWebSocketHandler, "/log").setAllowedOriginPatterns("*");;
    }
 
}
