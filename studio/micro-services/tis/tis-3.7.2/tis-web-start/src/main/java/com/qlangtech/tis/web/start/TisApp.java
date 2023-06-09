/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.web.start;

import com.qlangtech.tis.web.start.JettyTISRunner.IWebAppContextSetter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

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
        TisAppLaunch.get().setRunMode(TisRunMode.Standalone);
        // 启动应用使用本地8080端口
        TisApp tisApp = new TisApp(TisSubModule.WEB_START.getLaunchPort(), (context) -> {
            context.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
            context.setInitParameter("org.eclipse.jetty.servlet.Default.welcomeServlets", "true");
        });
        tisApp.start(args);
    }

    public TisApp(TisSubModule subModule, IWebAppContextCollector webAppContextCollector) throws Exception {
        super();
        this.jetty = new JettyTISRunner((subModule.getLaunchPort()), webAppContextCollector);
    }


    public TisApp(TisSubModule subModule, IWebAppContextSetter contextSetter) throws Exception {
        super();
        this.jetty = new JettyTISRunner(subModule.servletContext, (subModule.getLaunchPort()), contextSetter);
    }

    public TisApp(int port, IWebAppContextSetter contextSetter) throws Exception {
        super();
        this.jetty = new JettyTISRunner(port, contextSetter);
        this.initContext();
    }

    public void addContext(String context, File contextDir) throws Exception {
        jetty.addContext(context, contextDir, false, false);
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
                if (APP_CONSOLE.equals(context) || TisSubModule.ZEPPELIN.moduleName.equals(context)) {
                    continue;
                } else {
                    logger.info("load context:{}", context);
                    this.jetty.addContext(contextDir);
                }
            }
        }

        this.addZeppelinContext(new File(root, TisSubModule.ZEPPELIN.moduleName));

        // '/' root 的handler必须要最后添加
        contextDir = new File(root, APP_CONSOLE);
        if (contextDir.exists() && contextDir.isDirectory()) {
            this.addRootContext(contextDir);
        }

        if (this.jetty.validateContextHandler()) {
            throw new IllegalStateException("handlers can not small than 1,web rootDir:" + root.getAbsolutePath());
        }
    }

    public void addZeppelinContext(File contextDir) throws IOException {

        if (TisAppLaunch.get().isZeppelinHomeSetted()) {
            if (contextDir.exists()) {
                this.initZeppelinContext(contextDir);
                TisAppLaunch.get().setZeppelinContextInitialized();
            }
        }
    }

    public void addRootContext(File contextDir) throws Exception {
        // root
        File webappFile = new File(this.jetty.getWebapp(contextDir), PATH_WEB_XML);
        if (!webappFile.exists()) {
            // 写入本地
            try (InputStream input = this.getClass().getResourceAsStream("/web.xml")) {
                Objects.requireNonNull(input, "web.xml inputstram can not be null");
                FileUtils.copyToFile(input, webappFile);
            }
        }
        this.jetty.addContext("/", contextDir, false, true);
    }

    private void initZeppelinContext(File contextDir) throws IOException {

        File libDir = new File(contextDir, "lib");

        List<URL> jars = new ArrayList<>();
        addJars(libDir, jars);

        File zeppelinLibDir = new File(TisAppLaunch.get().getZeppelinHome(), "lib");
        addJars(zeppelinLibDir, jars, (cfile) -> {
            return !cfile.getName().startsWith("zeppelin-server");
        });
        if (jars.isEmpty()) {
            throw new IllegalStateException("there is any jars in libDir:" + libDir.getAbsolutePath());
        }
        URLClassLoader clazzLoader = new URLClassLoader(jars.toArray(new URL[jars.size()]), this.getClass().getClassLoader());
        final ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        /**
         * <pre>
         *     To avoid Error below,Shall set the current classloader:
         * Caused by: java.lang.ClassNotFoundException: org.apache.commons.configuration2.XMLConfiguration
         * 	at java.net.URLClassLoader.findClass(URLClassLoader.java:382)
         * 	at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
         * 	at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:349)
         * 	at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
         * 	at java.lang.Class.forName0(Native Method)
         * 	at java.lang.Class.forName(Class.java:348)
         * 	at org.apache.commons.lang3.ClassUtils.getClass(ClassUtils.java:993)
         * 	at org.apache.commons.lang3.ClassUtils.getClass(ClassUtils.java:1059)
         * 	at org.apache.commons.lang3.ClassUtils.getClass(ClassUtils.java:1042)
         * 	at org.apache.commons.configuration2.beanutils.BeanHelper.loadClass(BeanHelper.java:508)
         * 	at org.apache.commons.configuration2.beanutils.BeanHelper.fetchBeanClass(BeanHelper.java:546)
         * </pre>
         */
        try {
            Thread.currentThread().setContextClassLoader(clazzLoader);
            ServiceLoader<IWebAppContextCollector>
                    appContextCollectors = ServiceLoader.load(IWebAppContextCollector.class, clazzLoader);
            for (IWebAppContextCollector appContext : appContextCollectors) {
                this.jetty.addContext(appContext);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentLoader);
        }
    }

    private void addJars(File libDir, List<URL> jars) throws MalformedURLException {
        addJars(libDir, jars, (f) -> true);
    }

    private void addJars(File libDir, List<URL> jars, FileFilter fileFilter) throws MalformedURLException {
        if (!libDir.exists()) {
            throw new IllegalStateException("libDir:" + libDir.getAbsolutePath());
        }
        Iterator<File> jarIt = FileUtils.iterateFiles(libDir, new String[]{"jar"}, false);
        File childFile = null;
        while (jarIt.hasNext()) {
            childFile = jarIt.next();
            if (!fileFilter.accept(childFile)) {
                continue;
            }
            jars.add(childFile.toURI().toURL());
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
