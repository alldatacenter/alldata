package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.constants.ErrorCode;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.vo.ImportPermissionsVO;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.exception.ClientException;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 权限导入 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@RequestMapping("permission/import")
@Slf4j
public class PermissionImportController extends BaseController {

    private static final String LOG_PRE = "[" + PermissionImportController.class.getSimpleName() + "] ";

    @Autowired
    private AuthPolicy authPolicy;

    @Autowired
    private TeslaUserService teslaUserService;

    /**
     * 权限导入
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaBaseResult importPermissions(@RequestBody @Valid ImportPermissionsVO param,
                                             BindingResult validation)
        throws  AuthProxyException, ClientException {
        checkValidationBindingResult(validation);

        log.info(LOG_PRE + "Received permission import request. param={}", TeslaGsonUtil.toJson(param));
        UserDO userDo = teslaUserService.getUserByLoginName(getUserName());
        assert userDo != null;
        authPolicy.getAuthServiceManager().importPermissions(userDo, param);
        return buildSucceedResult(ErrorCode.EMPTY_OBJ);
    }
}

