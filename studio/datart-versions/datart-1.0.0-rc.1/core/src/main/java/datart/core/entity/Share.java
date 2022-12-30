package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class Share extends BaseEntity {

    private String orgId;

    private String vizType;

    private String vizId;

    private String authenticationMode;

    private String rowPermissionBy;

    private String authenticationCode;

    private Date expiryDate;

    private String roles;

}