package org.dromara.cloudeon.websocket;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dao.ServiceRoleInstanceRepository;
import org.dromara.cloudeon.dto.WsSessionBean;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.entity.ServiceRoleInstanceEntity;
import org.dromara.cloudeon.service.LogService;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class LogWebSocketHandler extends TextWebSocketHandler implements WebSocketHandler {
    /**
     * 保存与本个WebSocket建立起连接的客户端，Map<wsSessionId, wsSession Instance>
     */
    //private static Map<String, WebSocketSession> livingSessions = new ConcurrentHashMap<>();
    private static Map<String, WsSessionBean> livingSessionMap = new ConcurrentHashMap<>(); //使用线程安全的Map

    @Resource
    private LogService logService;

    @Resource
    private ServiceRoleInstanceRepository roleInstanceRepository;

    @Resource
    private ClusterNodeRepository clusterNodeRepository;


    ThreadPoolExecutor threadPoolExecutor = ThreadUtil.newExecutor(5, 100);

    /**
     * 连接建立成功时调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocketServer连接建立成功：" + session.getId());
        //先把SessionId发给前端，规定好一个格式，方便前端判定
        session.sendMessage(new TextMessage("##sessionId:" + session.getId()));


    }

    /**
     * 通过roleinstanceid 查询所在节点，并建立ssh连接
     *
     * @param roleId
     * @return
     */
    private Session buildSshSessionByRoleInstanceId(int roleId) throws JSchException {
        ServiceRoleInstanceEntity serviceRoleInstanceEntity = roleInstanceRepository.findById(roleId).get();
        Integer nodeId = serviceRoleInstanceEntity.getNodeId();
        ClusterNodeEntity clusterNodeEntity = clusterNodeRepository.findById(nodeId).get();

        JSch jsch = new JSch();
        Session session = jsch.getSession(clusterNodeEntity.getSshUser(), clusterNodeEntity.getIp(), clusterNodeEntity.getSshPort());
        session.setConfig("StrictHostKeyChecking", "no");
        session.setPassword(clusterNodeEntity.getSshPassword());
        session.connect();
        return session;
    }

    /**
     * 当客户端有消息发来时调用
     *
     * @param session 客户端连接
     * @param message 传来的消息
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("WebSocketServer收到客户端" + session.getId() + "的消息：" + payload);
        if (StrUtil.isNotBlank(payload) && payload.startsWith("##roleId:")) {
            // 提取角色实例id
            String roleIdStr = payload.split(":")[1];
            Integer roleId = Integer.valueOf(roleIdStr);
            // 根据角色实例id建立ssh连接
            Session sshSession = null;
            try {
                sshSession = buildSshSessionByRoleInstanceId(roleId);
                //缓存当前已经创建的连接
                WsSessionBean wsSessionBean = new WsSessionBean();
                wsSessionBean.setWebSocketSession(session);
                wsSessionBean.setWsSessionId(session.getId());
                wsSessionBean.setSshSession(sshSession);
                livingSessionMap.put(session.getId(), wsSessionBean);

                //WebSocket的前后端建立连接成功，立即调用日志推动逻辑，将数据推送给客户端
                threadPoolExecutor.execute(() -> {
                    try {
                        logService.sendLog2BrowserClient(wsSessionBean, roleId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }

    /**
     * 当有出错信息时调用
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        closeSshSession(sessionId);
        if (session.isOpen()) {
            session.close();
        }
        livingSessionMap.remove(session.getId());

        log.info("WebSocketServer出现错误：" + session.getId() + exception);
    }

    /**
     * 关闭连接后调用
     *
     * @param session 连接
     * @param status  状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        closeSshSession(sessionId);
        livingSessionMap.remove(sessionId);
        log.info("WebSocketServer已关闭：" + sessionId);
    }

    private void closeSshSession(String sessionId) {
        WsSessionBean wsSessionBean = livingSessionMap.get(sessionId);
        if (wsSessionBean != null) {
            Session sshSession = wsSessionBean.getSshSession();
            if (sshSession != null) {
                sshSession.disconnect();
                if (!sshSession.isConnected()) {
                    log.info("SSH已断开连接：" + sshSession.getHost() + ":" + sshSession.getPort() + "  " + sshSession);
                }
            }
        }

    }

//    public boolean closeWebSocketServer(String sid) {
//        WsSessionBean wsSessionBean = livingSessionMap.get(sid);
//        if(null != wsSessionBean) {
//            try {
//                //关闭WebSocket、SSH的连接会话
//                wsSessionBean.getWebSocketSession().close();
//                jschService.destroyConnect(wsSessionBean.getSshSession());
//
//                return true;
//            } catch (IOException e) {
//                log.info("closeWebSocketServer出现异常："+e);
//            }
//        }
//
//        return false;
//    }

}
