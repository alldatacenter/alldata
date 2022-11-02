package com.alibaba.tesla.authproxy.web;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.AuthUtil;
import com.alibaba.tesla.authproxy.web.output.TeslaAuthResult;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@Slf4j
@Controller
@RequestMapping("auth/tesla")
public class AuthController {

    @Autowired
    AuthUtil authUtil;
    @Autowired
    private AuthProperties authProperties;
    @Autowired
    private TeslaUserService teslaUserService;

    /**
     * header 校验
     *
     * @param request
     * @return
     */
    @RequestMapping("/authHeader")
    @ResponseBody
    public TeslaBaseResult authHeader(HttpServletRequest request) {
        TeslaAuthResult result = new TeslaAuthResult();
        UserDO userDo = authUtil.getExtAppUser(request);
        if (userDo != null) {
            log.info("Tesla header auth success:{}", JSONObject.toJSONString(userDo));
            result.setEmpId(userDo.getEmpId());
            result.setCheckSuccess(true);
        } else {
            result.setCheckSuccess(false);
        }
        return TeslaResultFactory.buildSucceedResult(result);
    }

}
