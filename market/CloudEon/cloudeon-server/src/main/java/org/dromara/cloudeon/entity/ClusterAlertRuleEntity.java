package org.dromara.cloudeon.entity;

import lombok.Data;
import org.dromara.cloudeon.enums.AlertLevel;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@Data
@Entity
@Table(name = "ce_alert_rule_define")
public class ClusterAlertRuleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")    private Integer id;

    private Integer clusterId;

    private String ruleName;

    @Convert(converter = AlertLevelConverter.class)
    private AlertLevel alertLevel;

    private String promql;


    private String stackServiceName;
    private String stackRoleName;

    private Integer waitForFireDuration;
    private String alertInfo;

    private String alertAdvice;

    /**
     * 开始时间
     */
    private Date createTime;

    private Date updateTime;





}
