package datart.server.base.params;

import lombok.Data;

@Data
public class UserAttributeParam {
    private String id;

    private String userId;

    private String name;

    private String value;
}
