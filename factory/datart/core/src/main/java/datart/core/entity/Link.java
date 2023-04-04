package datart.core.entity;

import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Link extends BaseEntity {
    private String relType;

    private String relId;

    private String url;

    private Date expiration;
}