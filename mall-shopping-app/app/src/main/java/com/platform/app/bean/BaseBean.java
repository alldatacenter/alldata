package com.platform.app.bean;

import java.io.Serializable;

/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/06
 *     desc   :model的基类
 *     version: 1.0
 * </pre>
 */

public class BaseBean implements Serializable {

    protected int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
