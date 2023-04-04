/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.controller;

import datart.core.base.annotations.SkipLogin;
import datart.core.base.consts.Const;
import datart.server.base.dto.ResponseData;
import datart.server.base.params.UserLoginParam;
import datart.server.service.ExternalRegisterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

@Api
@Slf4j
@RestController
@RequestMapping(value = "/external/register")
public class ExternalRegisterController extends BaseController {

    private final ExternalRegisterService externalRegisterService;

    public ExternalRegisterController(ExternalRegisterService externalRegisterService) {
        this.externalRegisterService = externalRegisterService;
    }

    @ApiOperation(value = "External Login")
    @SkipLogin
    @PostMapping(value = "ldap", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseData<Object> ldapLogin(UserLoginParam loginParam, HttpServletResponse response) throws MessagingException, UnsupportedEncodingException {
        String token = externalRegisterService.ldapRegister(loginParam.getUsername(), loginParam.getPassword());
        if (StringUtils.isNotBlank(token)) {
            response.setHeader(Const.TOKEN, token);
            return ResponseData.success(null);
        }
        return ResponseData.failure("ldap login fail");
    }


}
