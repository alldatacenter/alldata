/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.web.simplemvc;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.master.web.simplemvc.conf.WebConfig;
import org.apache.inlong.tubemq.server.master.web.simplemvc.exception.TemplateNotFoundException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.GenericXmlApplicationContext;

public class RequestDispatcher {

    private static final String TYPE_LAYOUT = "layout";
    private static final String TYPE_SCREEN = "screen";
    private static final String TYPE_CONTROL = "control";
    private static final String SCREEN_PLACEHOLDER = "screen_placeholder";
    private final WebConfig config;
    private final TemplateEngine engine;
    private final HashMap<String, Action> actions = new HashMap<>();
    private final HashMap<String, String> templates = new HashMap<>();
    private final ThreadLocal<ControlTool> controlTools = new ThreadLocal<>();
    private GenericXmlApplicationContext applicationContext;

    /**
     * Construct RequestDispatcher from web config
     *
     * @param config  the web configure object
     */
    public RequestDispatcher(WebConfig config) {
        this.config = config;
        this.engine = new VelocityTemplateEngine(config);
    }

    private static String getFileExtName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot);
            }
        }
        return filename;
    }

    public void init() throws Exception {
        engine.init();
        loadActions();
        loadTemplates();
    }

    public void processRequest(RequestContext context) throws Exception {
        for (Map.Entry<String, Object> entry : config.getTools().entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }
        ControlTool control = controlTools.get();
        if (control == null) {
            control = new ControlTool(this);
            controlTools.set(control);
        }
        control.setRequestContext(context);
        context.put(TYPE_CONTROL, control);
        executeTarget(context, context.getTarget(), TYPE_SCREEN);
        executeTarget(context, context.getTarget(), TYPE_LAYOUT);
    }

    /**
     * Execute required method service
     * @param context   the context
     * @param target    the target information
     * @param type      the operation type
     * @throws Exception the exception
     */
    public void executeTarget(RequestContext context,
                              String target, String type) throws Exception {
        String targetKey = getActionKey(type, target);
        if (actions.containsKey(targetKey)) {
            actions.get(targetKey).execute(context);
            if (".do".equals(context.requestType())) {
                context.getResp().setStatus(HttpServletResponse.SC_OK);
                return;
            }
        }
        String templatePath = getTemplateName(type, target);
        if (TYPE_SCREEN.equals(type)) {
            if (context.isRedirected()) {
                if (TStringUtils.isNotEmpty(context.getRedirectTarget())) {
                    context.setTarget(context.getRedirectTarget());
                    context.setRedirectTarget(null);
                    context.setRedirectLocation(null);
                    executeTarget(context, context.getTarget(), type);
                } else {
                    context.getResp().sendRedirect(context.getRedirectLocation());
                }
            } else {
                if (!this.templates.containsKey(templatePath)) {
                    throw new TemplateNotFoundException(new StringBuilder(256)
                            .append("Invalid ").append(type)
                            .append(" template path:").append(templatePath).toString());
                }
                context.put(SCREEN_PLACEHOLDER, engine.renderTemplate(templatePath, context));
            }
        } else {
            String realTemplatePath = templatePath;
            if (!templates.containsKey(templatePath) && TYPE_LAYOUT.equals(type)) {
                realTemplatePath = getLayout(target);
            }
            if (realTemplatePath == null) {
                throw new TemplateNotFoundException(new StringBuilder(256).append("Invalid ")
                        .append(type).append(" template path:").append(templatePath).toString());
            }
            if (!realTemplatePath.equals(templatePath)) {
                String realTargetKey =
                        realTemplatePath.substring(0, realTemplatePath.indexOf(".vm"));
                if (actions.containsKey(realTargetKey)) {
                    actions.get(realTargetKey).execute(context);
                }
            }
            engine.renderTemplate(realTemplatePath, context, context.getResp().getWriter());
        }
    }

    /**
     * Build action key as "type/target"
     *
     * @param type    the type value
     * @param target  the target value
     * @return    the key
     */
    public String getActionKey(String type, String target) {
        return new StringBuilder(256).append(type)
                .append("/").append(target).toString();
    }

    /**
     * Build template name as "type/target.vm"
     *
     * @param type     the type value
     * @param target   the target value
     * @return         the result
     */
    public String getTemplateName(String type, String target) {
        return new StringBuilder(256).append(type)
                .append("/").append(target).append(".vm").toString();
    }

    /**
     * Get layout information
     * @param target  the target information
     * @return   the layout information
     */
    public String getLayout(String target) {
        String layout = null;
        String[] targetPaths = target.split("/");
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = targetPaths.length - 1; i >= 0; i--) {
            pathBuilder.append(TYPE_LAYOUT);
            pathBuilder.append("/");
            for (int j = 0; j < i; j++) {
                pathBuilder.append(targetPaths[j]);
                pathBuilder.append("/");
            }
            pathBuilder.append("default.vm");
            String path = pathBuilder.toString();
            if (this.templates.containsKey(path)) {
                layout = path;
                break;
            } else {
                pathBuilder.delete(0, pathBuilder.length());
            }
        }
        return layout;
    }

    private void loadActions() throws Exception {
        String actionPath = config.getActionPackage();
        if (config.getActions().size() != 0) {
            for (Map.Entry<String, Action> entry : config.getActions().entrySet()) {
                String actionKey = entry.getKey().substring(
                        actionPath.length() + 1).replaceAll("\\.", "/");
                actions.put(TStringUtils.toCamelCase(actionKey), entry.getValue());
            }
        } else {
            if (config.isSpringSupported()) {
                String[] resources = new String[config.getBeanFilePathList().size()];
                for (int i = 0; i < config.getBeanFilePathList().size(); i++) {
                    resources[i] = config.getBeanFilePathList().get(i);
                }
                this.applicationContext = new GenericXmlApplicationContext();
                ConfigurableListableBeanFactory beanFactory =
                        this.applicationContext.getBeanFactory();
                AutowiredAnnotationBeanPostProcessor autowiredProcessor =
                        new AutowiredAnnotationBeanPostProcessor();
                autowiredProcessor.setBeanFactory(beanFactory);
                beanFactory.addBeanPostProcessor(autowiredProcessor);

                CommonAnnotationBeanPostProcessor commonProcessor =
                        new CommonAnnotationBeanPostProcessor();
                commonProcessor.setBeanFactory(beanFactory);
                beanFactory.addBeanPostProcessor(commonProcessor);

                RequiredAnnotationBeanPostProcessor requiredProcessor =
                        new RequiredAnnotationBeanPostProcessor();
                requiredProcessor.setBeanFactory(beanFactory);
                beanFactory.addBeanPostProcessor(requiredProcessor);
                this.applicationContext.load(resources);
            }
            List<Class> actionClasses = getActionClasses(actionPath);
            for (Class clazz : actionClasses) {
                String actionKey = clazz.getName().substring(
                        actionPath.length() + 1).replaceAll("\\.", "/");
                Action action = null;
                if (config.isSpringSupported()) {
                    action =
                            (Action) this.applicationContext.getBeanFactory().autowire(clazz,
                                    AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
                } else {
                    action = (Action) clazz.newInstance();
                }
                actions.put(TStringUtils.toCamelCase(actionKey), action);
            }
        }
    }

    private void loadTemplates() throws Exception {
        String templatePath = this.config.getTemplatePath();
        List<File> fileList = getFileList(templatePath, ".vm");
        for (File file : fileList) {
            String filePath = file.toURI().getPath();
            String relativePath = filePath.substring(templatePath.length(), filePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            this.templates.put(relativePath, relativePath);
        }
    }

    private List<Class> getActionClasses(String packageName) throws Exception {
        List<Class> classList = new ArrayList<>();
        String path = packageName.replaceAll("\\.", "/");
        URL url = this.getClass().getClassLoader().getResource(path);
        if (url == null) {
            return Collections.emptyList();
        }
        List<File> actionClassFileList = getFileList(url.getFile(), ".class");
        for (File f : actionClassFileList) {
            String pathStr = f.getPath().replaceAll("/|\\\\", ".");
            Class clazz = Class.forName(
                    pathStr.substring(pathStr.indexOf(packageName), pathStr.length() - 6));
            if (Action.class.isAssignableFrom(clazz)) {
                classList.add(clazz);
            }
        }
        return classList;
    }

    private List<File> getFileList(String directory, String type) throws Exception {
        File file = new File(directory);
        List<File> fileList = new ArrayList<>();
        getFile(file, fileList, type);
        return fileList;
    }

    private void getFile(File file, List<File> fileList, String type) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    getFile(f, fileList, type);
                }
            }
        } else {
            if (".*".equals(type) || type.equals(getFileExtName(file.getPath()))) {
                fileList.add(file);
            }
        }
    }
}
