package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StoryboardUpdateParam extends VizUpdateParam{

    private String name;

    private String config;

    private String parentId;

    private Boolean isFolder;

    private Double index;

}