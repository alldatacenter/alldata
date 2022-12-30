package cn.datax.common.security.handler;

import cn.datax.common.core.R;
import cn.datax.common.utils.ResponseUtil;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DataAuthExceptionEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ResponseUtil.makeResponse(
                response, MediaType.APPLICATION_JSON_VALUE,
                HttpServletResponse.SC_UNAUTHORIZED, R.error(HttpServletResponse.SC_UNAUTHORIZED, "无效的Token"));
    }
}
