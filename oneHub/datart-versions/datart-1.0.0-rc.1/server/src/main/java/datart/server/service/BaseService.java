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

package datart.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import datart.core.common.Application;
import datart.core.common.MessageResolver;
import datart.core.entity.BaseEntity;
import datart.core.entity.User;
import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.security.manager.DatartSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class BaseService extends MessageResolver {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected DatartSecurityManager securityManager;

    protected AsyncAccessLogService accessLogService;

    protected RelRoleResourceMapperExt rrrMapper;

    private Map<Class<? extends BaseEntity>, BaseCRUDService<?, ?>> entityServiceMap;

    @Autowired
    public void setSecurityManager(DatartSecurityManager datartSecurityManager) {
        this.securityManager = datartSecurityManager;
    }

    @Autowired
    public void setAccessLogService(AsyncAccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }

    @Autowired
    public void setRrrMapper(RelRoleResourceMapperExt rrrMapper) {
        this.rrrMapper = rrrMapper;
    }

    public AsyncAccessLogService getAccessLogService() {
        return accessLogService;
    }

    public User getCurrentUser() {
        return securityManager.getCurrentUser();
    }

    public RelRoleResourceMapperExt getRRRMapper() {
        return rrrMapper;
    }

    /**
     * 检查某类型的数据是否在对应表中存在，不存在则抛出 NotFoundException
     *
     * @param id  数据ID
     * @param clz 要调用的service类型
     */
    public void requireExists(String id, Class<? extends BaseEntity> clz) {
        getEntityService(clz).requireExists(id);
    }

    public <T> T retrieve(String id, Class<T> clz) {
        return (T) getEntityService(clz).retrieve(id);
    }

    public <T> T retrieve(String id, Class<T> clz, boolean checkPermission) {
        return (T) getEntityService(clz).retrieve(id, checkPermission);
    }

    private BaseCRUDService<?, ?> getEntityService(Class<?> clz) {
        if (CollectionUtils.isEmpty(entityServiceMap) || !entityServiceMap.containsKey(clz)) {
            entityServiceMap = new HashMap<>();
            Map<String, BaseCRUDService> beansOfType = Application.getContext().getBeansOfType(BaseCRUDService.class);
            for (BaseCRUDService service : beansOfType.values()) {
                entityServiceMap.put(service.getEntityClz(), service);
            }
        }
        return entityServiceMap.get(clz);
    }

    public MessageResolver getMessageResolver() {
        return this;
    }

    public DatartSecurityManager getSecurityManager() {
        return securityManager;
    }

}