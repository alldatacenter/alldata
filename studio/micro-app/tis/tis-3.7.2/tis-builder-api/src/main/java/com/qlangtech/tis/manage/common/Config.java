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
package com.qlangtech.tis.manage.common;


import com.qlangtech.tis.org.apache.commons.io.FileUtils;
import com.qlangtech.tis.utils.TisMetaProps;
import com.qlangtech.tis.web.start.TisAppLaunch;
import com.qlangtech.tis.web.start.TisSubModule;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class Config {
    public static final String KEY_ENV_TIS_HOME = "TIS_HOME";
    public static final String SYSTEM_KEY_LOGBACK_PATH_KEY = "logback.configurationFile";
    public static final String SYSTEM_KEY_LOGBACK_PATH_VALUE = "logback-datax.xml";
    public static final String SYSTEM_KEY__LOGBACK_HUDI = "logback-hudi.xml";
    public static final String KEY_TIS_PLUGIN_CONFIG = "tis_plugin_config";
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    public static final String LIB_PLUGINS_PATH = "libs/plugins";

    public static final String PLUGIN_LIB_DIR = "WEB-INF/lib";

    public static File getPluginLibDir(String pluginName, boolean validateExist) {
        File libDir = new File(Config.getDataDir(), Config.LIB_PLUGINS_PATH + "/" + pluginName + "/" + PLUGIN_LIB_DIR);
        if (validateExist && (!libDir.exists() || libDir.isFile())) {
            throw new IllegalStateException("dir " + pluginName + " is illegal:" + libDir.getAbsolutePath());
        }
        return libDir;
    }

    public static File getPluginLibDir(String pluginName) {
        return getPluginLibDir(pluginName, true);
    }


//    /**
//     * 通过在插件中的一个类找到插件的 WEB—INFO/lib 目录
//     *
//     * @param classInPlugin
//     * @return
//     */
//    public static File getPluginLibDir(Class<?> classInPlugin)  {
//        if (classInPlugin == null) {
//            throw new IllegalArgumentException("classInPlugin can not be null");
//        }
//        String clazz = classInPlugin.getName();
//
//        URL location = classInPlugin.getResource("/" + StringUtils.replace(clazz, ".", "/") + ".class");
//
//        if (location != null) {
//            final Pattern p = Pattern.compile("^.*file:(.+?" + PLUGIN_LIB_DIR + ").+?!.*$");
//            Matcher m = p.matcher(location.toString());
//            if (m.find()) {
//                //   return URLDecoder.decode(, "UTF-8");
//                return new File(m.group(1));
//            } else {
//                throw new IllegalStateException("location is illegal:" + location);
//            }
//            //   throw new ClassNotFoundException("Cannot parse location of '" + location + "'.  Probably not loaded from a Jar");
//        }
//        throw new IllegalStateException("Cannot find class '" + classInPlugin.getName() + " using the classloader");
//
//        // return getPluginLibDir(pluginName, true);
//    }


    public static final String TIS_PUB_PLUGINS_DOC_URL = "http://tis.pub/docs/plugin/plugins/#";

    private static final String bundlePath = "tis-web-config/config";
    private static final String KEY_TIS_DATASOURCE_TYPE = "tis.datasource.type";
    private static final String KEY_TIS_DATASOURCE_DBNAME = "tis.datasource.dbname";

    public static final String S4TOTALPAY = "search4totalpay";


    public static final String KEY_ZK_HOST = "zk.host";

    public static final String KEY_ASSEMBLE_HOST = "assemble.host";

    public static final String KEY_DEPLOY_MODE = "deploy.mode";

    public static final String KEY_TIS_HOST = "tis.host";
    public static final String KEY_PLUGIN_METAS = "pluginMetas";

    public static final String KEY_RUNTIME = "runtime";

    public static final String KEY_JAVA_RUNTIME_PROP_ENV_PROPS = "env_props";


    public static final String SUB_DIR_LIBS = "libs";

    public static final String SUB_DIR_CFG_REPO = "cfg_repo";
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_DERBY = "derby";

    public static final String QLANGTECH_PACKAGE = "com.qlangtech";

    private static final String GENERATE_PARENT_PACKAGE = QLANGTECH_PACKAGE + ".tis.realtime.transfer";

    public static final int LogFlumeAddressPORT = 41414;

    public static final String DEFAULT_DATA_DIR = "/opt/data/tis";

    private static Config config;

    public static final String KEY_DATA_DIR = "data.dir";


    public static TisMetaProps getMetaProps() {
        return TisMetaProps.getInstance();
    }

    public static void setDataDir(String path) {
        System.setProperty(KEY_DATA_DIR, path);
        getInstance().dataDir = new File(path);
    }

    public static File setTestDataDir() {
        String dataDir = null;
        if ((dataDir = System.getProperty(KEY_DATA_DIR)) != null) {
            throw new RuntimeException("dataDir:" + dataDir + " must be empty");
        }
        try {
            File tmp = new File("/tmp/tis");
            FileUtils.deleteQuietly(tmp);
            FileUtils.forceMkdir(tmp);
            Config.setDataDir(tmp.getAbsolutePath());
            return tmp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 在向分布式环境中传递任务时候要把A节点上的环境配置变量传输到B节点上去
     *
     * @param consumer
     */
    public void visitKeyValPair(Consumer<Map.Entry<String, String>> consumer) {
        Map<String, String> pairs = getImportKV();
        for (Map.Entry<String, String> e : pairs.entrySet()) {
            consumer.accept(e);
        }
    }

    private Map<String, String> getImportKV() {
        Map<String, String> pairs = new HashMap<String, String>() {
            @Override
            public String put(String key, String value) {
                if (StringUtils.isEmpty(value)) {
                    return null;
                }
                return super.put(key, value);
            }
        };
        pairs.put(KEY_ZK_HOST, this.zkHost);
        pairs.put(KEY_ASSEMBLE_HOST, this.assembleHost);
        pairs.put(KEY_TIS_HOST, this.tisHost);
        pairs.put(KEY_RUNTIME, this.runtime);
        pairs.put(KEY_TIS_DATASOURCE_TYPE, dbCfg.dbtype);
        pairs.put(KEY_TIS_DATASOURCE_DBNAME, dbCfg.dbname);
        pairs.put(KEY_DEPLOY_MODE, this.deployMode);
        pairs.put(TisAppLaunch.KEY_LOG_DIR, TisAppLaunch.getLogDir().getAbsolutePath());
        pairs.put(TisAppLaunch.KEY_TIS_LAUNCH_PORT, String.valueOf(TisSubModule.TIS_CONSOLE.getLaunchPort()));
        return pairs;
    }

    public Map<String, String> getAllKV() {
        HashMap<String, String> kvs = new HashMap<>(getImportKV());
        kvs.put(KEY_DATA_DIR, getDataDir(true).getAbsolutePath());
        return kvs;
    }

    /**
     * 当前部署方式是否是单机版
     *
     * @return
     */
    public static boolean isStandaloneMode() {
        return TisAppLaunch.isTestMock() || "standalone".equalsIgnoreCase(getInstance().deployMode);
    }

    /**
     * 本地基础配置目录
     *
     * @return
     */
    public static File getMetaCfgDir() {
        File dir = getDataDir();
        return new File(dir, SUB_DIR_CFG_REPO);
    }

    public static File getPluginCfgDir() {
        return new File(Config.getMetaCfgDir(), KEY_TIS_PLUGIN_CONFIG);
    }

    public static File getLibDir() {
        File dir = getDataDir();
        return new File(dir, SUB_DIR_LIBS);
    }

    public static File getDataDir() {
        return getDataDir(true);
    }

    public static File getTisHome() {
        File tisHome = new File(Objects.requireNonNull(System.getenv(KEY_ENV_TIS_HOME)
                , "env " + KEY_ENV_TIS_HOME + " has not been set"));
        if (!tisHome.exists() || !tisHome.isDirectory()) {
            throw new IllegalStateException("tisHomeDir:" + tisHome.getAbsolutePath() + " is not illegal");
        }
        return tisHome;
    }

    public static File getDataDir(boolean valiate) {
        File dir = null; //new File(System.getProperty(KEY_DATA_DIR, DEFAULT_DATA_DIR));
        try {
            dir = getInstance().dataDir;
        } catch (Throwable e) {
            logger.warn("can not get dataDir from config instance:" + e.getMessage());
            dir = new File(System.getProperty(KEY_DATA_DIR, DEFAULT_DATA_DIR));
        }
        if (valiate && !(dir.isDirectory() && dir.exists())) {
            throw new IllegalStateException("dir:" + dir.getAbsolutePath() + " is invalid DATA DIR");
        }
        return dir;
    }

    private final String zkHost;

    private final String tisHost;

    private final String runtime;

    private final String deployMode;

    private File dataDir;


    // 组装节点
    private final String assembleHost;

    private final TisDbConfig dbCfg;


    private static final Set<String> localDftValsKeys;

    static {
        localDftValsKeys = new HashSet<>();
        // localDftValsKeys.add(KEY_TIS_DATASOURCE_TYPE);
        localDftValsKeys.add(KEY_TIS_DATASOURCE_DBNAME);
    }

    private Config() {
        P p = P.create();

        this.zkHost = p.getString(KEY_ZK_HOST, true);
        this.assembleHost = p.getString(KEY_ASSEMBLE_HOST, true);
        this.tisHost = p.getString(KEY_TIS_HOST, true);
        this.runtime = p.getString(KEY_RUNTIME, true);

        this.deployMode = p.getString(KEY_DEPLOY_MODE);

        this.dataDir = new File(
                StringUtils.defaultIfEmpty(p.getString(KEY_DATA_DIR)
                        , System.getProperty(KEY_DATA_DIR, DEFAULT_DATA_DIR)));

        this.dbCfg = new TisDbConfig();
        try {
            dbCfg.dbtype = p.getString(KEY_TIS_DATASOURCE_TYPE, true);
            dbCfg.dbname = p.getString(KEY_TIS_DATASOURCE_DBNAME, true);

            if (!(DB_TYPE_MYSQL.equals(dbCfg.dbtype)
                    || DB_TYPE_DERBY.equals(dbCfg.dbtype))) {
                throw new IllegalStateException("dbCfg.dbtype:" + dbCfg.dbtype + " is illegal");
            }

            if (DB_TYPE_MYSQL.equals(dbCfg.dbtype)) {
                dbCfg.port = Integer.parseInt(p.getString("tis.datasource.port"));
                dbCfg.url = p.getString("tis.datasource.url");
                dbCfg.userName = p.getString("tis.datasource.username");
                dbCfg.password = p.getString("tis.datasource.password");
            }
        } catch (Exception e) {
            throw new IllegalStateException("please check the tis datasource cfg", e);
        }
    }


    public String getRuntime() {
        return this.runtime;
    }

    private static final ThreadLocal<String> threadContext = new ThreadLocal<>();

    public static void setThreadContextTisHost(String tishost) {
        threadContext.set(tishost);
    }

    public static String getConfigRepositoryHost() {
        String tisHost = null;
        if ((tisHost = threadContext.get()) == null) {
            tisHost = getInstance().tisHost;
        } else {
            threadContext.remove();
        }
        return "http://" + tisHost + ":" + (TisSubModule.TIS_CONSOLE.getLaunchPort()) + TisSubModule.TIS_CONSOLE.servletContext;
    }

    public static String getTisHost() {
        return getInstance().tisHost;
    }

    public static String getAssembleHost() {
        return getInstance().assembleHost;
    }

    public static String getAssembleHttpHost() {
        return "http://" + getAssembleHost()
                + ":" + (TisSubModule.TIS_ASSEMBLE.getLaunchPort()) + TisSubModule.TIS_ASSEMBLE.servletContext;
    }

    public static TisDbConfig getDbCfg() {
        return getInstance().getDbConfig();
    }

    public TisDbConfig getDbConfig() {
        return this.dbCfg;
    }

    public static String getZKHost() {
        String zkAddress = getInstance().getZkHost();
        if (StringUtils.isBlank(zkAddress)) {
            throw new IllegalStateException("zkAddress can not be null");
        }
        return zkAddress;
    }

    public static String getTerminatorRepositoryOnline() {
        throw new UnsupportedOperationException();
    }


    public static TestCfgStream openTestCfgStream() throws IOException {
        String propertiesFile = "tis-web-config/config.properties";
        File f = new File("../../../" + propertiesFile);
        if (f.exists()) {
            return new TestCfgStream(f);
        } else {
            f = new File("../../" + propertiesFile);
            if (f.exists()) {
                return new TestCfgStream(f);
            } else {
                f = new File("../" + propertiesFile);
                if (f.exists()) {
                    return new TestCfgStream(f);
                } else {
                    f = new File(propertiesFile);
                    return new TestCfgStream(f);
                }
            }
        }
    }

    public static class TestCfgStream {
        private final File propsFile;
        private InputStream propsStream;

        public InputStream getPropsStream() {
            return propsStream;
        }

        public File getPropsFile() {
            return propsFile;
        }

        public void validate(Throwable ee) {
            if (this.propsStream == null) {
                URL cpRoot = Config.class.getResource("/");
                URL cpThreadRoot = Thread.currentThread().getContextClassLoader().getResource("/");

                throw new IllegalStateException("file relevant stream is null,confFile:" + this.propsFile.getAbsolutePath()
                        + ",current classpath root url:" + String.valueOf(cpRoot) + " of classloader:" + Config.class.getClassLoader() + ",thread classpath:" + String.valueOf(cpThreadRoot), ee);
            }
            //Objects.requireNonNull(this.propsStream, "file relevant stream is null,confFile:" + this.propsFile.getAbsolutePath());
        }

        public TestCfgStream(File propsFile) {
            this.propsFile = propsFile;
            try {
                if (propsFile.exists()) {
                    this.setPropsStream(FileUtils.openInputStream(propsFile));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private TestCfgStream setPropsStream(InputStream propsStream) {
            this.propsStream = propsStream;
            return this;
        }
    }

    private abstract static class P {

        public static P create() {
            if (Boolean.getBoolean(KEY_JAVA_RUNTIME_PROP_ENV_PROPS)) {
                return new P() {
                    @Override
                    protected String getProp(String key) {
                        if (localDftValsKeys.contains(key)) {
                            return "defaultVal";
                        }
                        if (KEY_TIS_DATASOURCE_TYPE.equals(key)) {
                            return DB_TYPE_DERBY;
                        }
                        return StringUtils.defaultIfEmpty(System.getenv(key), System.getProperty(key));
                    }
                };
            } else {
                try {
                    ResourceBundle bundle = ResourceBundle.getBundle(bundlePath);
                    return new P() {
                        @Override
                        protected String getProp(String key) {
                            return bundle.getString(key);
                        }
                    };
                } catch (Throwable ee) {
                    // 测试环境中取工程目录下的配置文件
                    Properties props = new Properties();
                    try {
                        TestCfgStream cfgStream = openTestCfgStream();
                        cfgStream.validate(ee);
                        try (InputStream input = cfgStream.propsStream) {
                            props.load(input);
                        }
                        TisAppLaunch.setTest(true);
                        return new P() {
                            @Override
                            protected String getProp(String key) {
                                return props.getProperty(key);
                            }
                        };
                    } catch (IOException ex) {
                        StringBuffer errMsg = new StringBuffer();
                        errMsg.append("config file err:\n");
                        errMsg.append("has tried twice\n");
                        errMsg.append("second err:").append(ex.getMessage()).append("\n");
                        throw new RuntimeException(errMsg.toString());
                    }

                }
            }
        }

        public P() {
            super();
        }

        public final String getString(String key) {
            return getString(key, false);
        }

        public final String getString(String key, boolean notEmpty) {
            String value = null;
            try {
                value = getProp(key);
            } catch (Throwable e) {
            }
            if (StringUtils.isEmpty(value) && notEmpty) {
                throw new IllegalStateException("key:" + key + " relevant can not be empty");
            }
            return value;
        }

        protected abstract String getProp(String key);
    }

    public static String getGenerateParentPackage() {
        return GENERATE_PARENT_PACKAGE;
    }

    public String getZkHost() {
        return this.zkHost;
    }

    public static Config getInstance() {
        if (config == null) {
            synchronized (Config.class) {
                if (config == null) {
                    config = new Config();
                }
            }
        }
        return config;
    }

    public static void setConfig(Config cfg) {
        config = cfg;
    }

    public static class TisDbConfig {

        public String dbtype;
        public String dbname;

        //        tis.datasource.url=192.168.28.200
//        tis.datasource.username=root
//        tis.datasource.password=123456
//        tis.datasource.dbname=tis_console
        public int port;
        public String url;
        public String userName;
        public String password;

        @Override
        public String toString() {
            return "{" +
                    "dbtype='" + dbtype + '\'' +
                    ", dbname='" + dbname + '\'' +
                    ", port=" + port +
                    ", url='" + url + '\'' +
                    ", userName='" + userName + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }

    public static class FuncGroup {

        private final Integer key;

        private final String name;

        @Override
        public boolean equals(Object obj) {
            return this.hashCode() == obj.hashCode();
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        public FuncGroup(Integer key, String name) {
            super();
            this.key = key;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Integer getKey() {
            return key;
        }
    }
}
