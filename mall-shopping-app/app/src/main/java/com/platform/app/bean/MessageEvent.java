package com.platform.app.bean;

/**
 * Created by wulinhao
 * Time  2019/8/22
 * Describe: eventbus 的数据模型
 */

public class MessageEvent {

    public MessageEvent(int type) {
        this.type = type;
    }

    private int type = 0;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
