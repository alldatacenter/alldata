package org.dromara.cloudeon.enums;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum StepType {

    INSTALL_SERVICE_STEP(1,"安装",Lists.newArrayList(CommandType.INSTALL_SERVICE.name())),

    CONFIG_SERVICE_STEP(2,"配置",Lists.newLinkedList()),

    INIT_SERVICE_STEP(3,"初始化",Lists.newLinkedList()),

    START_SERVICE_STEP(4,"启动", Lists.newLinkedList()),




    ;

    private final int code;

    private final String name;
    private final List<String> list;

    public static StepType of(int code) {
        for (StepType nodeType : values()) {
            if (nodeType.code == code) {
                return nodeType;
            }
        }
        throw new IllegalArgumentException("unknown TaskGroupType of " + code);
    }

}
