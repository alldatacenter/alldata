package com.qlangtech.tis.plugin.ds;

import org.apache.commons.lang3.StringUtils;

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
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException(" param name can not be null");
        }
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
