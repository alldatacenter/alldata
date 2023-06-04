package datart.core.entity;

import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Download extends BaseEntity {
    private String name;

    private String path;

    private Date lastDownloadTime;

    private Byte status;
}