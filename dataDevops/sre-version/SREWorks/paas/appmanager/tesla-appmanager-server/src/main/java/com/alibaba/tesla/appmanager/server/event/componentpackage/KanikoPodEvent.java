package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.KanikoBuildEventEnum;
import org.springframework.context.ApplicationEvent;

/**
 * @ClassName: KanikoPodEvent
 * @Author: dyj
 * @DATE: 2021-07-14
 * @Description:
 **/
public class KanikoPodEvent extends ApplicationEvent {
    protected KanikoBuildEventEnum CURRENT_STATUS;
    protected String podName;

    public KanikoPodEvent(Object source, String obj) {
        super(source);
        this.podName = obj;
    }

    public KanikoBuildEventEnum getCurrentStatus() {
        return CURRENT_STATUS;
    }

    public String getPodName() {
        return podName;
    }
}
