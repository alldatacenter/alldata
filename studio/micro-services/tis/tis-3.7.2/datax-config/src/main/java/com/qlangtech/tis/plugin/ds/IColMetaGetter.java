package com.qlangtech.tis.plugin.ds;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-25 21:05
 **/
public interface IColMetaGetter {

    public static IColMetaGetter create(String name, DataType type) {
        return create(name, type, false);
    }

    public static IColMetaGetter create(String name, DataType type, boolean pk) {
        return new ColMeta(name, type, pk);
    }

    public String getName();

    public DataType getType();

    public boolean isPk();


}
