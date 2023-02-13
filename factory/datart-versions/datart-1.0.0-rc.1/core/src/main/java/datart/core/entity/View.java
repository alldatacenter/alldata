package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class View extends BaseEntity {
    private String name;

    private String description;

    private String orgId;

    private String sourceId;

    private String script;

    private String type;

    private String model;

    private String config;

    private String parentId;

    private Boolean isFolder;

    private Double index;

    private Byte status;
}