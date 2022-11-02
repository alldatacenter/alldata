package com.alibaba.tesla.appmanager.dynamicscript.core;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentActionEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDO;
import com.alibaba.tesla.appmanager.dynamicscript.service.DynamicScriptService;
import com.alibaba.tesla.appmanager.spring.util.SpringBeanUtil;
import groovy.lang.GroovyClassLoader;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.tesla.appmanager.common.constants.DefaultConstant.INTERNAL_ADDON_DEVELOPMENT_META;

@Component
@Slf4j
public class GroovyHandlerFactory {

    /**
     * Handler 版本映射
     */
    private static final ConcurrentHashMap<String, Integer> VERSION_MAP = new ConcurrentHashMap<>();

    /**
     * Handler 实例缓存
     */
    private static final ConcurrentHashMap<String, GroovyHandler> HANDLER_INSTANCES = new ConcurrentHashMap<>();

    private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    @Autowired
    private DynamicScriptService dynamicScriptService;

    /**
     * 定时检查并刷新 Groovy Handler
     */
    @Scheduled(cron = "${appmanager.cron-job.groovy-handler-factory-refresh:0 * * * * *}")
    public void cronRefresh() {
        List<String> keyList = new ArrayList<>(VERSION_MAP.keySet());
        for (String key : keyList) {
            KeyStore keyStore = keySplit(key);
            DynamicScriptQueryCondition condition = DynamicScriptQueryCondition.builder()
                    .kind(keyStore.getKind())
                    .name(keyStore.getName())
                    .build();
            DynamicScriptDO script = dynamicScriptService.get(condition);
            if (script == null) {
                log.warn("cannot get dynamic script from database|kind={}|name={}",
                        keyStore.getKind(), keyStore.getName());
                continue;
            }
            Integer revision = script.getCurrentRevision();
            if (!revision.equals(VERSION_MAP.getOrDefault(key, -1))) {
                synchronized (GroovyHandlerFactory.class) {
                    try {
                        create(keyStore.getKind(), keyStore.getName());
                        log.info("refresh groovy handler finished|kind={}|name={}", keyStore.getKind(), keyStore.getName());
                    } catch (AppException e) {
                        log.error("cannot refresh groovy handler|message={}", e.getErrorMessage());
                    }
                }
            } else {
                log.debug("no need to refresh groovy handler|kind={}|name={}", keyStore.getKind(), keyStore.getName());
            }
        }
    }

    /**
     * 启动时加载全量脚本
     */
    public void init() throws IOException {
        List<DynamicScriptDO> scripts = dynamicScriptService.list(DynamicScriptQueryCondition.builder().build());
        for (DynamicScriptDO script : scripts) {
            String kind = script.getKind();
            synchronized (GroovyHandlerFactory.class) {
                try {
                    create(kind, script.getName());
                } catch (Exception e) {
                    log.error("init groovy script failed, please check it|kind={}|name={}|exception={}",
                            kind, script.getName(), ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    /**
     * 获取当前全量的 Groovy Handler 列表
     *
     * @return GroovyHandlerItem List 对象
     */
    public List<GroovyHandlerItem> list() {
        List<GroovyHandlerItem> results = new ArrayList<>();
        for (Map.Entry<String, GroovyHandler> entry : HANDLER_INSTANCES.entrySet()) {
            KeyStore keyStore = keySplit(entry.getKey());
            results.add(GroovyHandlerItem.builder()
                    .kind(keyStore.getKind())
                    .name(keyStore.getName())
                    .groovyHandler(entry.getValue())
                    .build());
        }
        return results;
    }

    /**
     * 判断指定的 kind+name 对应的 Groovy 脚本是否存在
     *
     * @param kind 类型
     * @param name 名称
     * @return true or flase
     */
    public boolean exists(String kind, String name) {
        if (Objects.isNull(kind) || StringUtils.isEmpty(name)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "name cannot be empty");
        }

        String key = keyGenerator(kind, name);
        return HANDLER_INSTANCES.containsKey(key);
    }

    /**
     * 获取 Handler
     *
     * @param scriptClass 脚本 Class
     * @param kind        动态脚本类型
     * @param name        动态脚本唯一标识
     * @return Handler
     */
    public <T extends GroovyHandler> T get(Class<T> scriptClass, String kind, String name) {
        if (Objects.isNull(kind) || StringUtils.isEmpty(name)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "name cannot be empty");
        }

        // 先从缓存获取
        String key = keyGenerator(kind, name);
        GroovyHandler groovyHandler = HANDLER_INSTANCES.get(key);
        if (groovyHandler != null) {
            return scriptClass.cast(groovyHandler);
        }

        synchronized (GroovyHandlerFactory.class) {
            groovyHandler = HANDLER_INSTANCES.get(key);
            if (groovyHandler != null) {
                return scriptClass.cast(groovyHandler);
            }
            groovyHandler = create(kind, name);
        }
        return scriptClass.cast(groovyHandler);
    }

    /**
     * 根据 ComponentType 获取 Handler
     *
     * @param scriptClass   脚本 Class
     * @param appId         应用 ID (TODO: 后续每个应用可以自己自定义逻辑)
     * @param componentType 组件类型
     * @param componentName 组件名称
     * @return Handler 如果不支持，返回 null
     */
    public <T extends GroovyHandler> T getByComponentType(
            Class<T> scriptClass, String appId, ComponentTypeEnum componentType, String componentName,
            ComponentActionEnum action) {
        assert ComponentActionEnum.BUILD.equals(action) || ComponentActionEnum.DEPLOY.equals(action);
        switch (componentType) {
            case K8S_MICROSERVICE:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_MICROSERVICE_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_MICROSERVICE_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case K8S_JOB:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_JOB_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_JOB_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case ABM_OPERATOR_TVD:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_ABM_OPERATOR_TVD_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_ABM_OPERATOR_TVD_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case HELM:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_HELM_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_HELM_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case ABM_CHART:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_ABM_CHART_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_ABM_CHART_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case ASI_COMPONENT:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_ASI_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_ASI_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case ABM_KUSTOMIZE:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_ABM_KUSTOMIZE_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_ABM_KUSTOMIZE_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case ABM_HELM:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_ABM_HELM_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_ABM_HELM_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case ABM_STATUS:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_ABM_STATUS_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_ABM_STATUS_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case ABM_ES_STATUS:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_ABM_ES_STATUS_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_ABM_ES_STATUS_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            case INTERNAL_ADDON:
                if ("tianji_productops".equals(componentName)) {
                    if (ComponentActionEnum.BUILD.equals(action)) {
                        return get(scriptClass,
                                DynamicScriptKindEnum.BUILD_IA_TIANJI_PRODUCTOPS_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    } else {
                        return get(scriptClass,
                                DynamicScriptKindEnum.DEPLOY_IA_TIANJI_PRODUCTOPS_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    }
                }else if ("productops".equals(componentName)) {
                    if (ComponentActionEnum.BUILD.equals(action)) {
                        return get(scriptClass,
                                DynamicScriptKindEnum.BUILD_IA_PRODUCTOPS_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    } else {
                        return get(scriptClass,
                                DynamicScriptKindEnum.DEPLOY_IA_PRODUCTOPS_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    }
                } else if ("productopsv2".equals(componentName)) {
                    if (ComponentActionEnum.BUILD.equals(action)) {
                        return get(scriptClass,
                                DynamicScriptKindEnum.BUILD_IA_V2_PRODUCTOPS_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    } else {
                        return get(scriptClass,
                                DynamicScriptKindEnum.DEPLOY_IA_V2_PRODUCTOPS_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    }
                } else if ("appmeta".equals(componentName)) {
                    if (ComponentActionEnum.BUILD.equals(action)) {
                        return get(scriptClass,
                                DynamicScriptKindEnum.BUILD_IA_APP_META_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    } else {
                        return get(scriptClass,
                                DynamicScriptKindEnum.DEPLOY_IA_APP_META_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    }
                } else if (INTERNAL_ADDON_DEVELOPMENT_META.equals(componentName)) {
                    if (ComponentActionEnum.BUILD.equals(action)) {
                        return get(scriptClass,
                                DynamicScriptKindEnum.BUILD_IA_DEVELOPMENT_META_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    } else {
                        return get(scriptClass,
                                DynamicScriptKindEnum.DEPLOY_IA_DEVELOPMENT_META_COMPONENT.toString(),
                                DefaultConstant.DEFAULT_GROOVY_HANDLER);
                    }
                } else {
                    return null;
                }
            case RESOURCE_ADDON:
                if (ComponentActionEnum.BUILD.equals(action)) {
                    return get(scriptClass,
                            DynamicScriptKindEnum.BUILD_RESOURCE_ADDON_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                } else {
                    return get(scriptClass,
                            DynamicScriptKindEnum.DEPLOY_RESOURCE_ADDON_COMPONENT.toString(),
                            DefaultConstant.DEFAULT_GROOVY_HANDLER);
                }
            default:
                return null;
        }
    }

    /**
     * 创建 Handler
     *
     * @param kind 动态脚本类型
     * @param name 动态脚本唯一标识
     * @return Handler
     */
    private GroovyHandler create(String kind, String name) {
        String key = keyGenerator(kind, name);
        DynamicScriptQueryCondition condition = DynamicScriptQueryCondition.builder()
                .kind(kind)
                .name(name)
                .withBlobs(true)
                .build();
        DynamicScriptDO script = dynamicScriptService.get(condition);
        if (script == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find handler|kind=%s|name=%s", kind, name));
        }

        try {
            String code = script.getCode();
            Class<?> clazz = groovyClassLoader.parseClass(code);
            Object handler = clazz.getDeclaredConstructor().newInstance();
            VERSION_MAP.put(key, script.getCurrentRevision());
            populateHandler(handler);
            HANDLER_INSTANCES.put(key, (GroovyHandler) handler);
            log.info("groovy handler has loaded|kind={}|name={}|revision={}",
                    kind, name, script.getCurrentRevision());
            return (GroovyHandler) handler;
        } catch (Exception e) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    String.format("parse class from database failed in groovy loading progress|" +
                            "kind=%s|name=%s", kind, name), e);
        }
    }

    /**
     * 依赖注入
     *
     * @param handler handler
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    private static void populateHandler(Object handler) throws IllegalAccessException {
        if (handler == null) {
            return;
        }

        Field[] fields = handler.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Object fieldBean = null;
            if (AnnotationUtils.getAnnotation(field, Autowired.class) != null) {
                Qualifier qualifier = AnnotationUtils.getAnnotation(field, Qualifier.class);
                if (qualifier != null && qualifier.value().length() > 0) {
                    fieldBean = SpringBeanUtil.getApplicationContext().getBean(qualifier.value());
                } else {
                    fieldBean = SpringBeanUtil.getApplicationContext().getBean(field.getType());
                }
            }

            if (fieldBean != null) {
                field.setAccessible(true);
                field.set(handler, fieldBean);
            }
        }
    }

    /**
     * 生成 Map 的 Key 字符串
     *
     * @param kind 动态脚本类型
     * @param name 动态脚本名称
     * @return Key String
     */
    private static String keyGenerator(String kind, String name) {
        return String.format("%s|%s", kind, name);
    }

    /**
     * 将 Key 还原回 Kind + Name 的形式
     *
     * @param key Map Key
     * @return Keytore 对象
     */
    private static KeyStore keySplit(String key) {
        String[] array = key.split("\\|");
        if (array.length != 2) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid groovy handler key " + key);
        }
        return KeyStore.builder()
                .kind(array[0])
                .name(array[1])
                .build();
    }
}

/**
 * Key 存储
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
class KeyStore {

    /**
     * 类型
     */
    private String kind;

    /**
     * 名称
     */
    private String name;
}