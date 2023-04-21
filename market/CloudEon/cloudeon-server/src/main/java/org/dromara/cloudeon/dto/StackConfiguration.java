package org.dromara.cloudeon.dto;

import lombok.Data;

import java.util.List;
@Data
public class StackConfiguration {
    private String name;
    private String description;
    private String label;
    private String tag;
    private String recommendExpression;
    private String valueType;
    private boolean configurableInWizard;
    private String confFile;
    private Integer min;
    private Integer max;
    /**
     * 单位
     */
    private String unit;
    /**
     * 是否密码
     */
    private boolean isPassword;
    /**
     * 是否多值输入。像多了路径：/hdfs/path1,/hdfs/path2
     */
    private boolean isMultiValue;
    private List<String> options;
}
