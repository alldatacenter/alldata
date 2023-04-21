package org.dromara.cloudeon.dto;

import com.jcraft.jsch.Session;
import org.springframework.web.socket.WebSocketSession;

/**
 * 每个WebSocketSession关联的相关数据对象
 */
public class WsSessionBean {
    /** WebSocket Client与Server的连接会话ID */
    private String wsSessionId;

    /** WebSocket Client与Server的连接 */
    private WebSocketSession webSocketSession;

    /** SSH连接 */
    private Session sshSession;

    public String getWsSessionId() {
        return wsSessionId;
    }

    public void setWsSessionId(String wsSessionId) {
        this.wsSessionId = wsSessionId;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public Session getSshSession() {
        return sshSession;
    }

    public void setSshSession(Session sshSession) {
        this.sshSession = sshSession;
    }
}
