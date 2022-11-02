package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.util.ResponseUtil;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * <p>Description: 重写SpringMVC默认的error重定向处理，将重定向改成返回json数据 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Controller
@Slf4j
public class ErrorRedirectController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    private ErrorAttributes errorAttributes;

    @Autowired
    public ErrorRedirectController(ErrorAttributes errorAttributes) {
        Assert.notNull(errorAttributes, "ErrorAttributes must not be null");
        this.errorAttributes = errorAttributes;
    }

    /**
     * 处理error请求，将错误码和错误信息返回json
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "/error")
    @ResponseBody
    public void handleError(HttpServletRequest request, HttpServletResponse response) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode != null) {
            RequestAttributes requestAttributes = new ServletRequestAttributes(request);
            Map<String, Object> msg = this.errorAttributes.getErrorAttributes(requestAttributes, false);
            String errorMsg = Constants.RES_MSG_SERVERERROR;
            if (null != msg && msg.size() > 0) {
                if (msg.get("message") != null) {
                    errorMsg = msg.get("message").toString();
                } else {
                    errorMsg = TeslaGsonUtil.toJson(msg);
                }
            }
            if (!statusCode.equals(404)) {
                log.error("Error request, statusCode={}, errorMsg={}", statusCode, errorMsg);
            }
            ResponseUtil.writeErrorJson(response, statusCode, null, errorMsg);
        } else {
            ResponseUtil.writeErrorJson(response, TeslaResult.FAILURE, null, Constants.RES_MSG_SERVERERROR);
        }
    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }
}
