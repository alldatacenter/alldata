package com.alibaba.tesla.appmanager.server.listener;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.service.DynamicScriptService;
import com.alibaba.tesla.appmanager.server.service.informer.InformerManager;
import com.alibaba.tesla.appmanager.spring.event.ApplicationContextLoadedEvent;
import groovy.lang.GroovyClassLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Application Context 属性加载完毕后触发 Groovy Handler Factory 的初始化
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class ApplicationContextLoadedEventListener implements ApplicationListener<ApplicationContextLoadedEvent> {

    /**
     * 默认加载的 Groovy 文件
     */
    private final List<String> DEFAULT_GROOVY_SCRIPTS = Arrays.asList(
            "/dynamicscripts/DefaultBuildMicroserviceHandler.groovy",
            "/dynamicscripts/DefaultDeployMicroserviceHandler.groovy",
            "/dynamicscripts/DefaultBuildJobHandler.groovy",
            "/dynamicscripts/DefaultDeployJobHandler.groovy",
            "/dynamicscripts/HelmBuildMicroserviceHandler.groovy",
            "/dynamicscripts/HelmDeployMicroserviceHandler.groovy",
            "/dynamicscripts/DefaultBuildInternalAddonProductopsHandler.groovy",
            "/dynamicscripts/DefaultDeployInternalAddonProductopsHandler.groovy",
            "/dynamicscripts/DefaultBuildInternalAddonDevelopmentMetaHandler.groovy",
            "/dynamicscripts/DefaultDeployInternalAddonDevelopmentMetaHandler.groovy",
            "/dynamicscripts/DefaultBuildInternalAddonAppBindingHandler.groovy",
            "/dynamicscripts/DefaultDeployInternalAddonAppBindingHandler.groovy",
            "/dynamicscripts/DefaultBuildInternalAddonAppMetaHandler.groovy",
            "/dynamicscripts/DefaultDeployInternalAddonAppMetaHandler.groovy",
            "/dynamicscripts/DefaultInternalAddonV2ProductopsBuildHandler.groovy",
            "/dynamicscripts/DefaultInternalAddonV2ProductopsDeployHandler.groovy",
            "/dynamicscripts/DefaultBuildResourceAddonHandler.groovy",
            "/dynamicscripts/DefaultDeployResourceAddonHandler.groovy",
            "/dynamicscripts/TraitHostAliases.groovy",
            "/dynamicscripts/TraitHostNetwork.groovy",
            "/dynamicscripts/TraitSystemEnv.groovy",
            "/dynamicscripts/TraitNodeSelector.groovy",
            "/dynamicscripts/MicroserviceComponentWatchKubernetesInformerHandler.groovy",
            "/dynamicscripts/JobComponentWatchKubernetesInformerHandler.groovy",
            "/dynamicscripts/MicroserviceComponentHandler.groovy",
            "/dynamicscripts/MicroserviceComponentDestroyHandler.groovy",
            "/dynamicscripts/JobComponentHandler.groovy",
            "/dynamicscripts/JobComponentDestroyHandler.groovy",
            "/dynamicscripts/HelmComponentHandler.groovy",
            "/dynamicscripts/HelmComponentDestroyHandler.groovy",
            "/dynamicscripts/InternalAddonV2ProductopsComponentHandler.groovy",
            "/dynamicscripts/InternalAddonV2ProductopsComponentDestroyHandler.groovy",
            "/dynamicscripts/WorkflowDeployHandler.groovy",
            "/dynamicscripts/WorkflowRemoteDeployHandler.groovy",
            "/dynamicscripts/WorkflowSuspendHandler.groovy",
            "/dynamicscripts/PolicyTopologyHandler.groovy",
            "/dynamicscripts/PolicyOverrideHandler.groovy"
    );

    private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    @Autowired
    private GroovyHandlerFactory groovyHandlerFactory;

    @Autowired
    private DynamicScriptService dynamicScriptService;

    @Autowired
    private InformerManager informerManager;

    @Override
    public void onApplicationEvent(ApplicationContextLoadedEvent event) {
        try {
            initDefaultScripts();
            groovyHandlerFactory.init();
            informerManager.init();
        } catch (Exception e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot init groovy handler factory", e);
        }
    }

    private String readFileContent(String filePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(filePath)).getFile());
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes);
    }

    /**
     * 搜索 resources 下的自带默认执行器
     */
    private void initDefaultScripts() {
        for (String groovyPath : DEFAULT_GROOVY_SCRIPTS) {
            ClassPathResource groovyResource = new ClassPathResource(groovyPath);
            try {
                String code;
                try {
                    code = IOUtils.toString(groovyResource.getInputStream(), StandardCharsets.UTF_8);
                } catch (FileNotFoundException e) {
                    code = readFileContent(groovyPath);
                }
                Class<?> clazz = groovyClassLoader.parseClass(code);
                String kind = null, name = null;
                Integer revision = null;
                for (Field f : clazz.getFields()) {
                    if (f.getType().equals(String.class) || f.getType().equals(Integer.class)) {
                        String key = f.getName();
                        Object value = f.get(null);
                        switch (key) {
                            case "KIND":
                                kind = (String) value;
                                break;
                            case "NAME":
                                name = (String) value;
                                break;
                            case "REVISION":
                                revision = (Integer) value;
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (StringUtils.isEmpty(kind) || StringUtils.isEmpty(name) || revision == null) {
                    throw new RuntimeException(String.format("invalid groovy handler %s", groovyPath));
                }
                DynamicScriptQueryCondition condition = DynamicScriptQueryCondition.builder()
                        .kind(kind)
                        .name(name)
                        .build();
                dynamicScriptService.initScript(condition, revision, code);
            } catch (Exception e) {
                throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                        String.format("cannot initialize default scripts in resources directory: %s", groovyPath), e);
            }
        }
    }
}
