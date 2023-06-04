package org.dromara.cloudeon.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class ServiceConfVO {

    /**
     * 是否自定义配置
     */
    private Boolean isCustomConf;
    private String name;
    private String description;
    private String label;
    private String recommendExpression;
    private String valueType;
    private Boolean configurableInWizard;
    private String confFile;
    private Integer min;
    private Integer max;
    private String tag;
    /**
     * 单位
     */
    private String unit;
    /**
     * 是否密码
     */
    private Boolean isPassword;
    /**
     * 是否多值输入。像多了路径：/hdfs/path1,/hdfs/path2
     */
    private Boolean isMultiValue;
    private List<String> options;

}
