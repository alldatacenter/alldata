package com.alibaba.tesla.appmanager.server.addon.task.event;

import com.alibaba.tesla.appmanager.common.enums.AddonInstanceTaskEventEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * addon instance task 事件
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class AddonInstanceTaskEvent extends ApplicationEvent {

    @Getter
    private Long taskId;

    @Getter
    private AddonInstanceTaskEventEnum eventType;

    public AddonInstanceTaskEvent(Object source) {
        super(source);
    }

    public AddonInstanceTaskEvent(Object source, Long taskId, AddonInstanceTaskEventEnum eventType) {
        super(source);
        this.taskId = taskId;
        this.eventType = eventType;
    }
}
