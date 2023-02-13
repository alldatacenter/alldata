package cn.datax.auth.translator;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

@JsonSerialize(using = DataOauthExceptionSerializer.class)
public class DataOauthException extends OAuth2Exception {

    public DataOauthException(String msg) {
        super(msg);
    }
}
