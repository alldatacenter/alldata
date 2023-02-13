package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Storyboard extends BaseEntity {
    private String name;

    private String orgId;

    private String config;

    private String parentId;

    private Boolean isFolder;

    private Double index;

    private Byte status;
}