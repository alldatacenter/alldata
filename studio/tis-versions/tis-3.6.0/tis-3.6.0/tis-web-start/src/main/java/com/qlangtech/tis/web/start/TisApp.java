/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.web.start;

import com.qlangtech.tis.web.start.JettyTISRunner.IWebAppContextSetter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class TisApp {

    static {
        System.setProperty("logback.ContextSelector", "JNDI");
    }

    public static final String KEY_WEB_ROOT_DIR = "web.root.dir";

    private static Logger logger = LoggerFactory.getLogger(TisApp.class);

    private final JettyTISRunner jetty;


    public static void main(String[] args) throws Exception {

        if (TriggerStop.isStopCommand(args)) {
            int stopPort = Integer.parseInt(System.getProperty("STOP.PORT"));
            final String key = System.getProperty("STOP.KEY");
            TriggerStop.stop("127.0.0.1", stopPort, key, 5);
            return;
        }

        // 启动应用使用本地8080端口
        TisApp tisApp = new TisApp(TisAppLaunch.getPort(TisSubModule.WEB_START), (context) -> {
            context.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
            context.setInitParameter("org.eclipse.jetty.servlet.Default.welcomeServlets", "true");
        });
        tisApp.start(args);
    }

    public TisApp(TisSubModule subModule, IWebAppContextSetter contextSetter) throws Exception {
        super();
        this.jetty = new JettyTISRunner(subModule.servletContext, TisAppLaunch.getPort(subModule), contextSetter);
    }

    public TisApp(int port, IWebAppContextSetter contextSetter) throws Exception {
        super();
        this.jetty = new JettyTISRunner(port, contextSetter);
        this.initContext();
    }

    static final String APP_CONSOLE = "root";

    static final String PATH_WEB_XML = "WEB-INF/web.xml";

    private void initContext() throws Exception {
        File root = getWebRootDir();
        logger.info("webapps context dir:{}", root.getAbsolutePath());
        File contextDir = null;
        for (String context : root.list()) {
            contextDir = new File(root, context);
            if (contextDir.isDirectory() && !TisSubModule.WEB_START.moduleName.equals(context)) {
                if (APP_CONSOLE.equals(context)) {
                    continue;
                } else {
                    logger.info("load context:{}", context);
                    this.jetty.addContext(contextDir);
                }
            }
        }
        // '/' root 的handler必须要最后添加
        contextDir = new File(root, APP_CONSOLE);
        if (contextDir.exists() && contextDir.isDirectory()) {
            // root
            File webappFile = new File(this.jetty.getWebapp(contextDir), PATH_WEB_XML);
            if (!webappFile.exists()) {
                // 写入本地
                try (InputStream input = this.getClass().getResourceAsStream("/web.xml")) {
                    Objects.requireNonNull(input, "web.xml inputstram can not be null");
                    FileUtils.copyToFile(input, webappFile);
                }
            }
            this.jetty.addContext("/", contextDir, false);
        }

        if (this.jetty.validateContextHandler()) {
            throw new IllegalStateException("handlers can not small than 1,web rootDir:" + root.getAbsolutePath());
        }
    }

    private File getWebRootDir() {
        File root = new File(System.getProperty(KEY_WEB_ROOT_DIR, "."));
        if (!root.exists()) {
            throw new IllegalStateException("web.root.dir not exist:" + root.getAbsolutePath());
        }
        return root;
    }



    public void start(String[] args) throws Exception {
        logger.info("start TIS with port:{}", this.jetty.getPort());
        this.jetty.start();
    }
}
