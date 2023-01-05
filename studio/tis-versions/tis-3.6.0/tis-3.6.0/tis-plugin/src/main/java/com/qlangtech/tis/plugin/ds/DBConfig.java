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
package com.qlangtech.tis.plugin.ds;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.citrus.turbine.impl.DefaultContext;
import com.alibaba.fastjson.annotation.JSONField;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.runtime.module.misc.impl.AdapterMessageHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DBConfig implements IDbMeta {

    private static final Logger logger = LoggerFactory.getLogger(DBConfig.class);

    private String dbType;

    private String name;

    private final JdbcUrlBuilder jdbcUrlBuilder;

    // private String userName;

    // private String password;

    // private int port = 3306;

    private Map<String, List<String>> /* host|ip */ dbEnum = new HashMap<>();

    public void setDbEnum(Map<String, List<String>> dbEnum) {
        this.dbEnum = dbEnum;
    }

    private StringBuffer hostDesc;

    public DBConfig(JdbcUrlBuilder jdbcUrlBuilder) {
        this.jdbcUrlBuilder = jdbcUrlBuilder;
    }

    public DBConfig() {
        this(null);
    }

    @Override
    @JSONField(serialize = false)
    public String getFormatDBName() {
        return getFormatDBName(this.name);
    }

    @JSONField(serialize = false)
    public String getDAOJarName() {
        return getDAOJarName(this.name);
    }


    /**
     * 首字母小写并且将下划线去掉
     *
     * @param dbName
     * @return
     */
    public static String getFormatDBName(String dbName) {
        if (StringUtils.isEmpty(dbName)) {
            throw new IllegalArgumentException("param dbName can not be null");
        }
        return StringUtils.remove(StringUtils.lowerCase(dbName), "_");
    }

    public static String getDAOJarName(String dbName) {
        return getFormatDBName(dbName) + "-dao.jar";
    }

    /**
     * 概括性描述
     *
     * @return
     */
    public StringBuffer getHostDesc() {
        return this.hostDesc;
    }

    public void setHostDesc(StringBuffer hostDesc) {
        this.hostDesc = hostDesc;
    }

    /**
     * 遍历所有的db
     *
     * @param p
     */
    public void vistDbName(IProcess p) throws Exception {
        for (Map.Entry<String, List<String>> entry : dbEnum.entrySet()) {
            for (String dbname : entry.getValue()) {
                if (p.visit(this, entry.getKey(), dbname)) {
                    return;
                }
            }
        }
    }

    public void vistDbURL(boolean resolveHostIp, IDbUrlProcess urlProcess) {
        this.vistDbURL(resolveHostIp, urlProcess, false);
    }

    public void vistDbURL(boolean resolveHostIp, IDbUrlProcess urlProcess, boolean facade) {
        String[] err = new String[1];
        if (!this.vistDbURL(resolveHostIp, urlProcess, facade, new AdapterMessageHandler() {
            @Override
            public void addErrorMessage(Context context, String msg) {
                err[0] = msg;
            }
        }, new DefaultContext())) {
            throw new IllegalStateException("error:" + err[0]);
        }
    }

    /**
     * 遍历所有的jdbc URL
     */
    public boolean vistDbURL(boolean resolveHostIp, IDbUrlProcess urlProcess, boolean facade, IMessageHandler msgHandler, Context context) {
        final ExecutorService fixedThreadPool = Executors.newCachedThreadPool((runnable) -> {
            Thread t = new Thread(runnable);
//            t.setUncaughtExceptionHandler((tt, e) -> {
//                logger.error(e.getMessage(), e);
//            });
            return t;
        });
        try {
            int dbCount = 0;
            for (Map.Entry<String, List<String>> entry : this.getDbEnum().entrySet()) {
                dbCount += entry.getValue().size();
            }
            final CountDownLatch countDownLatch = new CountDownLatch(facade ? 1 : dbCount);
            int hostCount = 0;
            AtomicReference<String> fjdbcUrl = new AtomicReference<>();
            AtomicReference<Throwable> exceptionCollect = new AtomicReference<Throwable>();
            outer:
            for (Map.Entry<String, List<String>> entry : getDbEnum().entrySet()) {
                for (String dbName : entry.getValue()) {
                    // TODO 访问mysql的方式，将来如果有其他数据库可以再扩展一下
                    // String jdbcUrl = "jdbc:mysql://" + (resolveHostIp ? getHostIpAddress(entry.getKey()) : entry.getKey()) + ":" + this.getPort() + "/" + dbName + "?useUnicode=yes&characterEncoding=utf8";
                    String dbHost = resolveHostIp ? getHostIpAddress(entry.getKey()) : entry.getKey();
                    String jdbcUrl = this.jdbcUrlBuilder.buidJdbcUrl(this, dbHost, dbName);
                    hostCount++;
                    fixedThreadPool.execute(() -> {
                        try {
                            fjdbcUrl.set(jdbcUrl);
                            urlProcess.visit((facade ? name : dbName), dbHost, jdbcUrl);
                        } catch (Throwable e) {
                            exceptionCollect.set(e);
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                    if (facade) {
                        break outer;
                    }
                }
            }
            try {
                final int expireSec = 15;
                if (!countDownLatch.await(expireSec, TimeUnit.SECONDS)) {
                    msgHandler.addErrorMessage(context, "连接" + expireSec + "秒,超时:" + fjdbcUrl.get());
                    return false;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (exceptionCollect.get() != null) {
                throw new RuntimeException(exceptionCollect.get());
            }
            return true;
        } finally {
            try {
                fixedThreadPool.shutdownNow();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * 将host转成IP地址
     *
     * @param ip
     * @return
     */
    private String getHostIpAddress(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public String getUserName() {
//        return userName;
//    }
//
//    public void setUserName(String userName) {
//        this.userName = userName;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//
//    public int getPort() {
//        return port;
//    }

//    public void setPort(int port) {
//        this.port = port;
//    }

    @JSONField(serialize = false)
    public Map<String, List<String>> getDbEnum() {
        return dbEnum;
    }

    public void addDbName(String host, String dbname) {
        List<String> dbNames = this.dbEnum.get(host);
        if (dbNames == null) {
            dbNames = new ArrayList<>();
            this.dbEnum.put(host, dbNames);
        }
        dbNames.add(dbname);
    }

    public interface IProcess {

        /**
         * true: stop visit
         *
         * @param config
         * @param ip
         * @param dbName
         * @return
         */
        boolean visit(DBConfig config, String ip, String dbName) throws Exception;
    }

    public interface IDbUrlProcess {

        /**
         * true: stop visit
         *
         * @param dbName
         * @return
         */
        void visit(String dbName, String dbHost, String jdbcUrl);
    }

//    @Override
//    public String toString() {
//        return "{" + "name='" + name + '\'' + "password='" + "******" + '\'' + ", userName='" + userName + '\'' + ", port=" + port + ", hostDesc=" + hostDesc + '}';
//    }
}
