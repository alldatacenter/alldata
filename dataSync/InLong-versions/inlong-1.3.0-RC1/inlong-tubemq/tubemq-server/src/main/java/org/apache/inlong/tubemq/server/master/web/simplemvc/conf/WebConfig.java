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

package org.apache.inlong.tubemq.server.master.web.simplemvc.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.master.web.simplemvc.Action;

public class WebConfig {

    private final HashMap<String, Object> tools = new HashMap<>();
    private final HashMap<String, Action> actions = new HashMap<>();
    private final HashSet<String> types = new HashSet<>();
    private String resourcePath;
    private String templatePath;
    private String actionPackage;
    private String velocityConfigFilePath;
    private String supportedTypes = ".htm,.html";
    private String defaultPage = "index.htm";
    private boolean springSupported = false;
    private List<String> beanFilePathList = new ArrayList<>();
    private boolean standalone = false;

    public WebConfig() {
        parseTypes(this.supportedTypes);
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
        if (TStringUtils.isEmpty(this.templatePath)) {
            this.templatePath = ("/".equals(resourcePath) ? "" : resourcePath) + "/templates";
        }
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getDefaultPage() {
        return defaultPage;
    }

    public void setDefaultPage(String defaultPage) {
        this.defaultPage = defaultPage;
    }

    public String getActionPackage() {
        return actionPackage;
    }

    public void setActionPackage(String actionPackage) {
        this.actionPackage = actionPackage;
    }

    public String getVelocityConfigFilePath() {
        return velocityConfigFilePath;
    }

    public void setVelocityConfigFilePath(String velocityConfigFilePath) {
        this.velocityConfigFilePath = velocityConfigFilePath;
    }

    public String getSupportedTypes() {
        return supportedTypes;
    }

    public void setSupportedTypes(String supportedTypes) {
        this.supportedTypes = supportedTypes;
        parseTypes(supportedTypes);
    }

    public HashMap<String, Object> getTools() {
        return tools;
    }

    public HashMap<String, Action> getActions() {
        return actions;
    }

    public void registerAction(Action action) {
        actions.put(action.getClass().getName(), action);
    }

    public void registerTool(String id, Object tool) {
        tools.put(id, tool);
    }

    public boolean containsType(String type) {
        return types.contains(type);
    }

    private void parseTypes(String typePattern) {
        String[] typeArr = typePattern.split(",");
        types.clear();
        Collections.addAll(types, typeArr);
    }

    public List<String> getBeanFilePathList() {
        return beanFilePathList;
    }

    public void setBeanFilePathList(List<String> beanFilePathList) {
        this.beanFilePathList = beanFilePathList;
    }

    public boolean isSpringSupported() {
        return springSupported;
    }

    public void setSpringSupported(boolean springSupported) {
        this.springSupported = springSupported;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    public void addBeanFilePath(String path) {
        this.beanFilePathList.add(path);
    }
}
