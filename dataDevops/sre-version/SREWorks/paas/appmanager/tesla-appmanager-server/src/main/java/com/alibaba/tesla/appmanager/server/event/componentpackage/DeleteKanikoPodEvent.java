package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.KanikoBuildEventEnum;

/**
 * @ClassName: DeleteKanikoPodEvent
 * @Author: dyj
 * @DATE: 2021-07-15
 * @Description:
 **/
public class DeleteKanikoPodEvent extends KanikoPodEvent {
    public DeleteKanikoPodEvent(Object source, String obj) {
        super(source, obj);
        this.CURRENT_STATUS = KanikoBuildEventEnum.DELETE;
    }
}
