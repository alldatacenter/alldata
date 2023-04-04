package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class Schedule extends BaseEntity {
    private String name;

    private String orgId;

    private String type;

    private Boolean active;

    private String cronExpression;

    private Date startDate;

    private Date endDate;

    private String config;

    private String parentId;

    private Boolean isFolder;

    private Double index;

    private Byte status;
}