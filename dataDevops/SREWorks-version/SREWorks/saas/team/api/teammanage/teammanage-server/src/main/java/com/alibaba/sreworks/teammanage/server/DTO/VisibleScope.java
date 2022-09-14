package com.alibaba.sreworks.teammanage.server.DTO;

/**
 * @author jinghua.yjh
 */

public enum VisibleScope {

    //全部可见
    PUBLIC("全部可见", 0),

    //仅团队成员可见
    INTERNAL("仅团队成员可见", 1);

    private final String cn;

    private final int level;

    public String getCn() {
        return this.cn;
    }

    public int getLevel() {
        return this.level;
    }

    VisibleScope(String cn, int level) {
        this.cn = cn;
        this.level = level;
    }

}
