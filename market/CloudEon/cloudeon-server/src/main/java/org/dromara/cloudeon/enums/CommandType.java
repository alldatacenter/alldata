package org.dromara.cloudeon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommandType {

    INSTALL_SERVICE(1,"初始化服务"),

    START_SERVICE(2,"启动服务"),

    STOP_SERVICE(3,"停止服务"),

    DELETE_SERVICE(4,"删除服务"),

    START_ROLE(5,"启动角色"),

    STOP_ROLE(6,"停止角色"),

    DELETE_ROLE(7,"删除角色"),

    RESTART_SERVICE(8,"重启服务"),
    UPGRADE_SERVICE_CONFIG(9,"刷新服务配置"),



    ;

    private final int code;

    private final String desc;

    public static CommandType of(int code) {
        for (CommandType nodeType : values()) {
            if (nodeType.code == code) {
                return nodeType;
            }
        }
        throw new IllegalArgumentException("unknown CommandType of " + code);
    }

}
