package com.alibaba.tesla.appmanager.domain.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

import lombok.Data;

/**
 * @ClassName: CustomAddonWorkloadSpec
 * @Author: dyj
 * @DATE: 2020-12-22
 * @Description:
 **/
@Data
public class CustomAddonWorkloadSpec implements Serializable {
    private static final long serialVersionUID = 86436255778877475L;
    private Long appPackageId;
    private DeployAppSchema applicationConfig;
    private List<Parameter> parameterValues = new ArrayList<>();
    private JSONObject customOutputs;

    @Data
    public static class Parameter implements Serializable{
        private static final long serialVersionUID = 3657116190965794696L;
        private String name;
        private Boolean require;
        private Object defaultValue;
        private Object value = "";
        private List<String> fieldPath;

    }
}
