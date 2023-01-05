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
package com.qlangtech.tis.coredefine.module.action;

import com.koubei.abator.AdapterUserDefineProperties;
import com.koubei.abator.KoubeiIbatorRunner.PojoExtendsClass;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.manage.common.incr.StreamContextConstant;
import com.qlangtech.tis.plugin.ds.FacadeDataSource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 生成DAO层代码的上下文
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年10月8日
 */
public class IbatorProperties extends AdapterUserDefineProperties {

    private final List<String> tables;

    private final Properties props;

    private static String PARENT_PACKAGE = "com.qlangtech.tis.realtime";

    private static final Logger logger = LoggerFactory.getLogger(IbatorProperties.class);

    private File daoDir;

    private final String databaseName;

    private final boolean daoScriptCreated;
    private final DataSource dataSource;

    public IbatorProperties(FacadeDataSource facadeDataSource, List<String> tables, long timestamp) {
        super();
        if (facadeDataSource == null) {
            throw new IllegalArgumentException("param dbConfig can not be null");
        }

        this.props = new Properties();
        this.dataSource = facadeDataSource.dataSource;
        if (timestamp < 1) {
            throw new IllegalArgumentException("param timestamp:" + timestamp + " can not small than 1");
        }
        // new File(Config.getDataDir() + "/" + StreamContextConstant.DIR_DAO + "/" + dbConfig.getFormatDBName(), String.valueOf(timestamp));
        this.daoDir = StreamContextConstant.getDAORootDir(facadeDataSource.dbMeta.getName(), timestamp);
        this.daoScriptCreated = this.daoDir.exists();
        try {
            FileUtils.forceMkdir(daoDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("db dao generate local dir:" + daoDir.getAbsolutePath());
        this.props.setProperty(KEY_outputDir, daoDir.getAbsolutePath());
        // 取所有分库中的第一个
//        dbConfig.vistDbURL(false, new IDbUrlProcess() {
//
//            @Override
//            public void visit(String dbName, String jdbcUrl) {
//                props.setProperty(KEY_databaseUrl, jdbcUrl);
//            }
//        });
//        if (StringUtils.isBlank(props.getProperty(KEY_databaseUrl))) {
//            throw new IllegalStateException("key:" + KEY_databaseUrl + " relevant val can not be null");
//        }
//        this.props.setProperty(KEY_password, dbConfig.getPassword());
//        this.props.setProperty(KEY_username, dbConfig.getUserName());
        this.props.setProperty(KEY_project, PARENT_PACKAGE + "." + facadeDataSource.dbMeta.getFormatDBName());
        this.tables = tables;
        this.databaseName = facadeDataSource.dbMeta.getName();
    }

    /**
     * dao 脚本对应的timestamp是否被创建过
     *
     * @return
     */
    public boolean isDaoScriptCreated() {
        return this.daoScriptCreated;
    }

    @Override
    public String getDatabaseName() {
        return this.databaseName;
    }

    public File getDaoDir() {
        return this.daoDir;
    }

    @Override
    public boolean generateDataSourceConfig() {
        return false;
    }

    @Override
    public DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public Properties getProperty() {
        return this.props;
    }

    @Override
    public String getTables() {
        return tables.stream().collect(Collectors.joining(","));
    }

    @Override
    public String getOutputfileEncode() {
        return TisUTF8.getName();
    }

    @Override
    public boolean isBooleanMapEnable() {
        return false;
    }

    @Override
    public boolean isDisableGenerateModifyDAOMethod() {
        return true;
    }

    @Override
    public String getDependencyPackageParentName() {
        return "com.qlangtech.tis.ibatis";
    }

    @Override
    public PojoExtendsClass getPojoExtendsClass() {
        return new PojoExtendsClass("com.qlangtech.tis.realtime.transfer.AbstractRowValueGetter", "AbstractRowValueGetter");
    }
}
