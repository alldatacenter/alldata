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


import datart.core.base.exception.Exceptions;
import datart.core.common.MessageResolver;
import datart.core.entity.User;
import datart.security.manager.DatartSecurityManager;
import datart.core.base.exception.ParamException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseController extends MessageResolver {

    protected DatartSecurityManager securityManager;

    @Autowired
    public void setSecurityManager(DatartSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    protected User getCurrentUser() {
        return securityManager.getCurrentUser();
    }

    public void checkBlank(String param, String paramName) {
        if (StringUtils.isBlank(param)) {
            Exceptions.tr(ParamException.class, "error.param.empty", paramName);
        }
    }

}
