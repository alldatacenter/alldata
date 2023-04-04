package datart.security.base;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class JwtToken implements Serializable {

    private String subject;

    private Date exp;

    private int pwdHash;

    private Long createTime;

}
