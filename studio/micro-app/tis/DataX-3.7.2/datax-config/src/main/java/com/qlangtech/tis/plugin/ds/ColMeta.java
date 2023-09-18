package com.qlangtech.tis.plugin.ds;

import java.io.Serializable;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-25 17:45
 **/
public class ColMeta implements IColMetaGetter, Serializable {
    public final String name;
    public final DataType type;
    public final boolean pk;

    public ColMeta(String name, DataType type, boolean pk) {
        this.name = name;
        this.type = type;
        this.pk = pk;
    }

    @Override
    public boolean isPk() {
        return this.pk;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataType getType() {
        return type;
    }
}
