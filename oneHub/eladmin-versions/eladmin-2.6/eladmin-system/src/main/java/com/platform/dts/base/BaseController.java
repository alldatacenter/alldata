package com.platform.dts.base;


import com.baomidou.mybatisplus.extension.api.ApiController;
import com.platform.dts.util.JwtTokenUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

import static com.platform.dts.core.util.Constants.STRING_BLANK;

/**
 *
 * @author AllDataDC
 * @date 2022/11/16 11:14
 * @Description: base controller
 **/
public class BaseController extends ApiController {

    public Integer getCurrentUserId(HttpServletRequest request) {
        Enumeration<String> auth = request.getHeaders(JwtTokenUtils.TOKEN_HEADER);
        String token = auth.nextElement().replace(JwtTokenUtils.TOKEN_PREFIX, STRING_BLANK);
        return JwtTokenUtils.getUserId(token);
    }
}
