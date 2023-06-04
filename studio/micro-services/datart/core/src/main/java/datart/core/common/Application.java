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

package datart.core.common;

import datart.core.base.consts.TenantManagementMode;
import datart.core.entity.Organization;
import datart.core.entity.User;
import datart.core.mappers.ext.OrganizationMapperExt;
import datart.core.mappers.ext.UserMapperExt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

import static datart.core.base.consts.TenantManagementMode.PLATFORM;

@Component
@Slf4j
public class Application implements ApplicationContextAware {

    private static ApplicationContext context;

    private static TenantManagementMode currMode;

    private static Boolean initialized;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        Application.context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> t) {
        return context.getBean(t);
    }

    public static <T> T getBean(String beanName, Class<T> t) {
        return context.getBean(beanName, t);
    }

    public static String getProperty(String key) {
        return context.getEnvironment().getProperty(key);
    }

    public static String getProperty(String key, String defaultVal) {
        return context.getEnvironment().getProperty(key, defaultVal);
    }

    public static String getFileBasePath() {
        String path = getProperty("datart.env.file-path");
        if (path.startsWith(".")) {
            path = path.replace(".", userDir());
        }
        return StringUtils.appendIfMissing(path, "/");
    }

    public static String userDir() {
        return StringUtils.removeEnd(System.getProperty("user.dir"), "/");
    }


    public static String getWebRootURL() {
        String url = getProperty("datart.server.address");
        url = StringUtils.removeEnd(url, "/");
        return url;
    }

    public static String getApiPrefix() {
        return getProperty("datart.server.path-prefix");
    }

    public static String getServerPrefix() {
        return getProperty("server.servlet.context-path","/");
    }

    public static String getTokenSecret() {
        return getProperty("datart.security.token.secret", "d@a$t%a^r&a*t");
    }

    public static boolean canRegister() {
        return BooleanUtils.toBoolean(getProperty("datart.user.register", "true"));
    }

    public static TenantManagementMode getCurrMode() {
        if (currMode == null) {
            String mode = Application.getProperty("datart.tenant-management-mode");
            try {
                return TenantManagementMode.valueOf(mode.toUpperCase());
            } catch (Exception e) {
                log.warn("Unrecognized tenant-management-mode: '{}', and this will run in platform tenant-management-mode", mode);
            }
            currMode = PLATFORM;
        }
        return currMode;
    }

    public static void setCurrMode(TenantManagementMode mode) {
        currMode = mode;
    }

    public static Boolean isInitialized() {
        if (initialized != null) {
            return initialized;
        }
        updateInitialized();
        return initialized;
    }

    public static void updateInitialized() {
        UserMapperExt userMapper = getBean(UserMapperExt.class);
        if (getCurrMode().equals(PLATFORM)) {
            initialized = userMapper.selectUserCount()>0;
        }
        OrganizationMapperExt orgMapper = getBean(OrganizationMapperExt.class);
        List<Organization> organizations = orgMapper.list();
        int orgCount = CollectionUtils.size(organizations);
        if (orgCount==0) {
            initialized = false;
        } else if (orgCount==1) {
            List<User> users = orgMapper.listOrgMembers(organizations.get(0).getId());
            initialized = users.size()>0;
        }
    }
}
