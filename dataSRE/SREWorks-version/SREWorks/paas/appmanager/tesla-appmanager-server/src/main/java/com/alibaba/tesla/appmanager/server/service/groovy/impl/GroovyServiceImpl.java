package com.alibaba.tesla.appmanager.server.service.groovy.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.req.groovy.GroovyUpgradeReq;
import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.service.DynamicScriptService;
import com.alibaba.tesla.appmanager.server.service.groovy.GroovyService;
import groovy.lang.GroovyClassLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

/**
 * Groovy 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class GroovyServiceImpl implements GroovyService {

    private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    @Autowired
    private DynamicScriptService dynamicScriptService;

    /**
     * 升级指定 code 到系统中加载
     *
     * @param req 脚本升级请求
     */
    public void upgradeScript(GroovyUpgradeReq req) {
        String kind = req.getKind();
        String name = req.getName();
        String code = req.getCode();
        if (StringUtils.isAnyEmpty(kind, name, code)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "kind/name/code is required");
        }

        try {
            Class<?> clazz = groovyClassLoader.parseClass(code);
            Integer revision = null;
            for (Field f : clazz.getFields()) {
                if (f.getType().equals(String.class) || f.getType().equals(Integer.class)) {
                    String key = f.getName();
                    Object value = f.get(null);
                    if ("REVISION".equals(key)) {
                        revision = (Integer) value;
                    }
                }
            }
            if (revision == null) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("invalid groovy code, revision is required|kind=%s|name=%s", kind, name));
            }
            DynamicScriptQueryCondition condition = DynamicScriptQueryCondition.builder()
                    .kind(kind)
                    .name(name)
                    .build();
            dynamicScriptService.initScript(condition, revision, code);
        } catch (Exception e) {
            log.error("cannot upgrade groovy script|kind={}|name={}|exception={}",
                    kind, name, ExceptionUtils.getStackTrace(e));
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("cannot upgrade groovy script|error=%s", e.getMessage()));
        }
    }
}
