package com.platform.admin.base;


import cn.datax.service.system.api.dto.JwtUserDto;
import cn.datax.service.system.api.feign.UserServiceFeign;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.platform.admin.util.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

import static com.platform.core.util.Constants.STRING_BLANK;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * base controller
 **/
@Component
public class BaseController extends ApiController {

    @Autowired
    UserServiceFeign userServiceFeign;

    public Long getCurrentUserId(HttpServletRequest request) {
        return 1L;
    }
}
