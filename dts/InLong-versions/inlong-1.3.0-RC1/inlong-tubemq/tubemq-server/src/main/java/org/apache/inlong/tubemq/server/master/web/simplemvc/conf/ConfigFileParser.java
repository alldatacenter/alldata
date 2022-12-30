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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import org.apache.inlong.tubemq.server.master.web.simplemvc.exception.InvalidConfigException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class ConfigFileParser {
    private static final String ROOT_ELEMENT = "simpleMVConfig";
    private static final String DEFAULT_PAGE = "default-page";
    private static final String RESOURCES = "resources";
    private static final String TEMPLATES = "templates";
    private static final String ACTIONS = "actions";

    private static final String TOOLS = "tools";
    private static final String TOOL = "tool";

    private static final String SPRING_SUPPORT = "spring-support";
    private static final String SPRING_IMPORT = "import";
    private static final String VELOCITY_SUPPORT = "velocity-support";

    private File configFile;

    public ConfigFileParser() {
    }

    public ConfigFileParser(File configFile) {
        this.configFile = configFile;
    }

    public static void main(String[] args) throws Exception {
        File configFile = new File("D:\\work\\WEB-INF\\simple-mvc.xml");
        ConfigFileParser parser = new ConfigFileParser(configFile);
        WebConfig config = parser.parse();
    }

    public WebConfig parse(File configFile) throws Exception {
        this.configFile = configFile;
        return parse();
    }

    /**
     * Parse website configure file information
     *
     * @return    the website configure object
     */
    public WebConfig parse() throws Exception {
        SAXReader reader = new SAXReader();
        WebConfig config = new WebConfig();
        InputStream in = new FileInputStream(configFile);
        try {
            Document document = reader.read(in);
            Element root = document.getRootElement();
            if (root == null || !root.getName().equals(ROOT_ELEMENT)) {
                throw new InvalidConfigException("Root element is null or invalid."
                        + "It must be '" + ROOT_ELEMENT + "'");
            }
            List<Element> elements = root.elements();
            for (Element e : elements) {
                configure(config, e);
            }
        } finally {
            try {
                in.close();
            } catch (Throwable e2) {
                //
            }
        }
        return config;
    }

    // #lizard forgives
    private void configure(WebConfig config, Element e) throws InvalidConfigException {
        if (config == null || e == null) {
            return;
        }
        String name = e.getName();
        if (DEFAULT_PAGE.equals(name)) {
            if (e.attribute("value") == null) {
                throw new InvalidConfigException(errorMsg("value", DEFAULT_PAGE));
            }
            config.setDefaultPage(e.attribute("value").getValue());
        } else if (RESOURCES.equals(name)) {
            if (e.attribute("path") == null) {
                throw new InvalidConfigException(errorMsg("path", RESOURCES));
            }
            config.setResourcePath(e.attribute("path").getValue());
        } else if (TEMPLATES.equals(name)) {
            if (e.attribute("path") == null) {
                throw new InvalidConfigException(errorMsg("path", TEMPLATES));
            }
            config.setTemplatePath(e.attribute("path").getValue());
        } else if (ACTIONS.equals(name)) {
            if (e.attribute("package") == null) {
                throw new InvalidConfigException(errorMsg("package", ACTIONS));
            }
            config.setActionPackage(e.attribute("package").getValue());
        } else if (TOOLS.equals(name)) {
            List<Element> elements = e.elements();
            for (Element e1 : elements) {
                configure(config, e1);
            }
        } else if (TOOL.equals(name)) {
            if (e.attribute("id") == null || e.attribute("class") == null) {
                throw new InvalidConfigException(errorMsg("id and class", TOOL));
            }
            try {
                Class clazz = Class.forName(e.attribute("class").getValue());
                config.registerTool(e.attribute("id").getValue(), clazz.newInstance());
            } catch (Throwable t) {
                throw new InvalidConfigException("Init tool failed!", t);
            }
        } else if (SPRING_SUPPORT.equals(name)) {
            if (e.attribute("value") == null) {
                throw new InvalidConfigException(errorMsg("value", SPRING_SUPPORT));
            }
            if ("true".equals(e.attribute("value").getValue())) {
                config.setSpringSupported(true);
                List<Element> elements = e.elements();
                for (Element e2 : elements) {
                    configure(config, e2);
                }
            }
        } else if (SPRING_IMPORT.equals(name)) {
            if (e.attribute("path") == null) {
                throw new InvalidConfigException(errorMsg("path", SPRING_IMPORT));
            }
            config.addBeanFilePath(e.attribute("path").getValue());
        } else if (VELOCITY_SUPPORT.equals(name)) {
            if (e.attribute("config-file") == null) {
                throw new InvalidConfigException(errorMsg("config-file", VELOCITY_SUPPORT));
            }
            config.setVelocityConfigFilePath(e.attribute("config-file").getValue());
        }
    }

    private String errorMsg(String attr, String elementName) {
        return new StringBuilder(256).append("Attribute '")
                .append(attr).append("' is needed for ").append(elementName).toString();
    }
}
