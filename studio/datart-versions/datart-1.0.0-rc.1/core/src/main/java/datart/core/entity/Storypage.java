package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Storypage extends BaseEntity {
    private String storyboardId;

    private String relType;

    private String relId;

    private String config;
}