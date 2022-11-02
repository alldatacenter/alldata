package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.KanikoBuildEventEnum;

/**
 * @ClassName: AddKanikoPodEvent
 * @Author: dyj
 * @DATE: 2021-07-14
 * @Description:
 **/
public class FailedKanikoPodEvent extends KanikoPodEvent {
    public FailedKanikoPodEvent(Object source, String obj) {
        super(source, obj);
        this.CURRENT_STATUS = KanikoBuildEventEnum.FAILED;
    }
}
