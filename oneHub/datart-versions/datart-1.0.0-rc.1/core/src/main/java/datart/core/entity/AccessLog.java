package datart.core.entity;

import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccessLog extends BaseEntity {
    private String user;

    private String resourceType;

    private String resourceId;

    private String accessType;

    private Date accessTime;

    private Integer duration;
}