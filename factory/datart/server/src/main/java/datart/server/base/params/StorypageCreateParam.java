package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorypageCreateParam extends VizCreateParam {

    private String storyboardId;

    private String relId;

    private String relType;

    private String config;

}
