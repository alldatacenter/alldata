package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SourceCreateParam extends BaseCreateParam{

    private String name;

    private String type;

    private String orgId;

    private String config;

    private String description;

    private String parentId;

    private Boolean isFolder;

    private Double index;

}
