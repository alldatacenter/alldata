package com.alibaba.tesla.appmanager.workflow.util;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Workflow 网络工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class WorkflowNetworkUtil {

    @Autowired
    private ServerProperties serverProperties;

    /**
     * 获取当前本机 IP 地址 + 启动端口
     *
     * @return 本机 IP 地址
     */
    public String getClientHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":" + serverProperties.getPort();
        } catch (UnknownHostException e) {
            throw new AppException(AppErrorCode.NETWORK_ERROR, "Cannot get current ip address", e);
        }
    }
}
