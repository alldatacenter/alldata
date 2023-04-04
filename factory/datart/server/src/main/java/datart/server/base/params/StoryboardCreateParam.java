package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StoryboardCreateParam extends VizCreateParam {

    private String name;

    private String config;

    private String parentId;

    private Boolean isFolder;

    private Double index;

}
