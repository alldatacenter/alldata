package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Organization extends BaseEntity {
    private String name;

    private String avatar;

    private String description;
}