package org.dromara.cloudeon.dto;

import lombok.Data;

import java.util.List;

@Data
public class StackServiceAlertRuleInfo {

    private List<StackServiceAlertRule> rules;
}
