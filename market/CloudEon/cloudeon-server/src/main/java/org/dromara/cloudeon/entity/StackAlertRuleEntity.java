package org.dromara.cloudeon.entity;

import lombok.Data;
import org.dromara.cloudeon.enums.AlertLevel;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;


@Data
@Entity
@Table(name = "ce_stack_alert_rule")
public class StackAlertRuleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")    private Integer id;
    /**
     * 框架id
     */
    private Integer stackId;

    private String ruleName;

    @Convert(converter = AlertLevelConverter.class)
    private AlertLevel alertLevel;

    private String promql;


    private String stackServiceName;
    private String stackRoleName;

    private Integer waitForFireDuration;
    private String alertInfo;

    private String alertAdvice;


   

}
