package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserSettingsCreateParam extends BaseCreateParam {

    private String relType;

    private String relId;

    private String config;

}