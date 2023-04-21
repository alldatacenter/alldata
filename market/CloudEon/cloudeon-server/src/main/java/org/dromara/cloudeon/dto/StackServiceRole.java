package org.dromara.cloudeon.dto;

import lombok.Data;

@Data
public class StackServiceRole {

    private String name;
    private String label;
    private String roleFullName;
    private String linkExpression;
    private String type;
    private Integer sortNum;
    private Integer jmxPort;
    private Integer minNum;
    private Integer fixedNum;
    private boolean needOdd;
    private String logFile;

}
