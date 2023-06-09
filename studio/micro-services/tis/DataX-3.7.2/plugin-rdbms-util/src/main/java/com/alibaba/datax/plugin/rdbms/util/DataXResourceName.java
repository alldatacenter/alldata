package com.alibaba.datax.plugin.rdbms.util;

import com.qlangtech.tis.datax.IDataXNameAware;
import com.qlangtech.tis.plugin.StoreResourceType;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-23 11:26
 **/
public class DataXResourceName {
    private final IDataXNameAware name;
    public final StoreResourceType resType;
    // public final DBIdentity dbFactoryId;

    public DataXResourceName(IDataXNameAware name, StoreResourceType resType) {
        this.name = name;
        this.resType = resType;
        //  this.dbFactoryId = dbFactoryId;
    }

    public String getDataXName() {
        return this.name.getTISDataXName();
    }

}
