package com.alibaba.tesla.appmanager.domain.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.tesla.appmanager.domain.schema.CustomAddonWorkloadSpec;

import lombok.Data;

/**
 * @ClassName: CustomWorkloadResource
 * @Author: dyj
 * @DATE: 2020-12-23
 * @Description:
 **/
@Data
public class CustomWorkloadResource implements Serializable {

    private static final long serialVersionUID = 7881942127685889587L;

    /**
     * API 版本号
     */
    private String apiVersion;

    /**
     * 类型
     */
    private String kind;

    /**
     * 元信息
     */
    private WorkloadResource.MetaData metadata;

    /**
     * 定义描述文件
     */
    private CustomAddonWorkloadSpec spec;

    /**
     * 数据输出列表
     */
    private List<WorkloadResource.DataOutput> dataOutputs = new ArrayList<>();

}
