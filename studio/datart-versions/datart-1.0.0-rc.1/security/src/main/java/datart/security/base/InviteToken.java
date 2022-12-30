package datart.security.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InviteToken extends JwtToken {

    private String inviter;

    private String orgId;

    private String userId;

}
