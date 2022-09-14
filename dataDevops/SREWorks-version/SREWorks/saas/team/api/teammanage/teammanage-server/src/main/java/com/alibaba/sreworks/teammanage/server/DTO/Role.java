package com.alibaba.sreworks.teammanage.server.DTO;

/**
 * @author jinghua.yjh
 */

public enum Role {

    //全部可见
    ADMIN("管理员", 0),

    //仅团队成员可见
    GUEST("成员", 1);

    private final String cn;

    private final int level;

    public String getCn() {
        return this.cn;
    }

    public int getLevel() {
        return this.level;
    }

    Role(String cn, int level) {
        this.cn = cn;
        this.level = level;
    }

}
