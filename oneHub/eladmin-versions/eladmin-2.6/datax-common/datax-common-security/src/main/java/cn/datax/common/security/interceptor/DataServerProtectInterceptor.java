package cn.datax.common.security.interceptor;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.R;
import cn.datax.common.utils.ResponseUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Base64Utils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DataServerProtectInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 从请求头中获取Token
        String token = request.getHeader(DataConstant.Security.TOKENHEADER.getVal());
        String gatewayToken = new String(Base64Utils.encode(DataConstant.Security.TOKENVALUE.getVal().getBytes()));
        // 校验Token的正确性
        if (StrUtil.equals(gatewayToken, token)) {
            return true;
        } else {
            ResponseUtil.makeResponse(
                    response, MediaType.APPLICATION_JSON_VALUE,
                    HttpServletResponse.SC_FORBIDDEN, R.error("请通过网关获取资源"));
            return false;
        }
    }
}
