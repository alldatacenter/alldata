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

package datart.server.service.impl;

import datart.core.base.exception.Exceptions;
import datart.core.common.Application;
import datart.server.base.dto.SystemInfo;
import datart.server.base.params.SetupParams;
import datart.server.service.SysService;
import datart.server.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Slf4j
@Service
public class SysServiceImpl implements SysService {

    @Value("${datart.security.token.timeout-min:30}")
    private String tokenTimeout;

    @Value("${datart.user.active.send-mail:false}")
    private boolean sendMail;

    @Override
    public SystemInfo getSysInfo() {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setTokenTimeout(tokenTimeout);
        systemInfo.setMailEnable(sendMail);
        systemInfo.setVersion(getVersion());
        systemInfo.setTenantManagementMode(Application.getCurrMode().name());
        systemInfo.setRegisterEnable(Application.canRegister());
        systemInfo.setInitialized(Application.isInitialized());
        return systemInfo;
    }

    private String getVersion() {
        try {
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
            JarFile jarFile = new JarFile(jarPath);
            Manifest manifest = jarFile.getManifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            return mainAttributes.getValue("Implementation-Version");
        } catch (Exception e) {
            return "dev";
        }
    }

    @Override
    public boolean setup(SetupParams params) throws MessagingException, UnsupportedEncodingException {
        Application.updateInitialized();
        if (Application.isInitialized()) {
            Exceptions.msg("The application already initialized.");
        }
        UserService userService = Application.getBean(UserService.class);
        boolean res = userService.setupUser(params.getUser());
        Application.updateInitialized();
        log.info("The application is initialized with User({}).", params.getUser().getUsername());
        return res;
    }

}
