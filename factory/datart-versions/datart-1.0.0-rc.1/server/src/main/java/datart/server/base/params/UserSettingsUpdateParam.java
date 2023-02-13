package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserSettingsUpdateParam extends BaseUpdateParam {

    private String relType;

    private String relId;

    private String config;

}