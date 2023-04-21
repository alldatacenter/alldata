package org.dromara.cloudeon.entity;

import org.dromara.cloudeon.enums.AlertLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 告警信息表
 *
 */
@Entity
@Data
@Table(name = "ce_alert_message")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertMessageEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;

    private String alertName;

    private String alertGroupName;

    private String alertInfo;

    private String alertAdvice;

    private String alertLabels;
    private String hostname;
    private Integer nodeId;
    @Convert(converter = AlertLevelConverter.class)
    private AlertLevel alertLevel;
    private boolean resolved;
    private Integer serviceRoleInstanceId;
    private Integer serviceInstanceId;
    private String fireTime;
    private String solveTime;
    private Integer clusterId;
    private Date createTime;
    private Date updateTime;






}
