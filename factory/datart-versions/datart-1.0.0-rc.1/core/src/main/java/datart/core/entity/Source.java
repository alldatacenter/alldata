package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Source extends BaseEntity {
    private String name;

    private String config;

    private String type;

    private String orgId;

    private String parentId;

    private Boolean isFolder;

    private Double index;

    private Byte status;
}