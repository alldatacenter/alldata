package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Datachart extends BaseEntity {

    private String name;

    private String description;

    private String viewId;

    private String orgId;

    private String config;

    private String thumbnail;

    private Byte status;
}