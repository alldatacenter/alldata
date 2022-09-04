package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.KanikoBuildEventEnum;

/**
 * @ClassName: UpdateKanikoPodEvent
 * @Author: dyj
 * @DATE: 2021-07-14
 * @Description:
 **/
public class SucceedKanikoPodEvent extends KanikoPodEvent {
    public SucceedKanikoPodEvent(Object source, String obj) {
        super(source, obj);
        this.CURRENT_STATUS = KanikoBuildEventEnum.SUCCEED;
    }
}
