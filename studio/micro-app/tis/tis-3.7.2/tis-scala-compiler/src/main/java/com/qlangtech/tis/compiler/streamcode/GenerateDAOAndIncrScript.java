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
package com.qlangtech.tis.compiler.streamcode;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.koubei.abator.KoubeiIbatorRunner;
import com.koubei.abator.KoubeiProgressCallback;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.compiler.java.IOutputEntry;
import com.qlangtech.tis.compiler.java.JavaCompilerProcess;
import com.qlangtech.tis.coredefine.module.action.IbatorProperties;
import com.qlangtech.tis.coredefine.module.action.IndexIncrStatus;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.incr.StreamContextConstant;
import com.qlangtech.tis.offline.DbScope;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import com.qlangtech.tis.plugin.ds.DataSourceFactoryPluginStore;
import com.qlangtech.tis.plugin.ds.FacadeDataSource;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.sql.parser.DBNode;
import com.qlangtech.tis.sql.parser.stream.generate.FacadeContext;
import org.apache.commons.io.FileUtils;
import org.apache.ibatis.ibator.config.IbatorContext;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class GenerateDAOAndIncrScript {

    private final IControlMsgHandler msgHandler;

    private final IndexStreamCodeGenerator indexStreamCodeGenerator;

    public GenerateDAOAndIncrScript(IControlMsgHandler msgHandler, IndexStreamCodeGenerator indexStreamCodeGenerator) {
        this.msgHandler = msgHandler;
        this.indexStreamCodeGenerator = indexStreamCodeGenerator;
    }

    /**
     * @param incrStatus
     * @param compilerAndPackage
     * @param dependencyDbs      Map<Integer,Long[DBID]>
     * @throws Exception
     */
    public void generate(Context context, IndexIncrStatus incrStatus, boolean compilerAndPackage, Map<Integer, Long> dependencyDbs) throws Exception {
        generateDAOScript(context, dependencyDbs);
        generateIncrScript(context, incrStatus, compilerAndPackage, Collections.unmodifiableMap(indexStreamCodeGenerator.getDbTables()), false);
    }

    public static boolean incrStreamCodeCompileFaild(File sourceRootDir) {
        File compileFaildToken = new File(sourceRootDir, IOutputEntry.KEY_COMPILE_FAILD_FILE);
        boolean exist = compileFaildToken.exists();
        try {
            if (exist) {
                // 如果不存在需要将之前的文件夹清空一下
                org.apache.commons.io.FileUtils.deleteDirectory(sourceRootDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return exist;
    }

    public void generateIncrScript(Context context, IndexIncrStatus incrStatus, boolean compilerAndPackage, Map<DBNode, List<String>> dbNameMap, boolean hasCfgChanged) {
        try {
            //final Map<DBNode, List<String>> dbNameMap = Collections.unmodifiableMap(indexStreamCodeGenerator.getDbTables());
            File sourceRoot = StreamContextConstant.getStreamScriptRootDir(
                    indexStreamCodeGenerator.collection, indexStreamCodeGenerator.incrScriptTimestamp);
            if (!indexStreamCodeGenerator.isIncrScriptDirCreated()
                    || // 检查Faild Token文件是否存在
                    incrStreamCodeCompileFaild(sourceRoot)
                    || hasCfgChanged) {
                /**
                 * *********************************************************************************
                 * 自动生成scala代码
                 * ***********************************************************************************
                 */
                indexStreamCodeGenerator.generateStreamScriptCode();
                // 生成依赖dao依赖元数据信息
                DBNode.dump(dbNameMap.keySet().stream().collect(Collectors.toList())
                        , StreamContextConstant.getDbDependencyConfigMetaFile(
                                indexStreamCodeGenerator.collection, indexStreamCodeGenerator.incrScriptTimestamp));
                /**
                 * *********************************************************************************
                 * 生成spring相关配置文件
                 * ***********************************************************************************
                 */
                indexStreamCodeGenerator.generateConfigFiles();
            }
            incrStatus.setIncrScriptMainFileContent(indexStreamCodeGenerator.readIncrScriptMainFileContent());
            // TODO 真实生产环境中需要 和 代码build阶段分成两步
            if (compilerAndPackage) {

                TISSinkFactory streamFactory = TISSinkFactory.getIncrSinKFactory(this.indexStreamCodeGenerator.collection);
//                 HeteroEnum.INCR_STREAM_CONFIG.getPluginStore(
//                        IPluginContext.namedContext(this.indexStreamCodeGenerator.collection), null);
                // (TISSinkFactory) pluginStore.getPlugin();
                Objects.requireNonNull(streamFactory, "relevant streamFactory can not be null,collection:" + this.indexStreamCodeGenerator.collection);
//                CompileAndPackage packager = new CompileAndPackage();
                streamFactory.getCompileAndPackageManager().process(context, this.msgHandler, indexStreamCodeGenerator.collection
                        , dbNameMap.entrySet().stream().collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue()))
                        , sourceRoot, indexStreamCodeGenerator.getSpringXmlConfigsObjectsContext());
            }
        } catch (Exception e) {
            // 将原始文件删除干净
            try {
                FileUtils.forceDelete(indexStreamCodeGenerator.getStreamCodeGenerator().getIncrScriptDir());
            } catch (Throwable ex) {
                // ex.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    private void generateDAOScript(Context context, Map<Integer, Long> dependencyDbs) throws Exception {
        final Map<DBNode, List<String>> dbNameMap = Collections.unmodifiableMap(indexStreamCodeGenerator.getDbTables());
        if (dbNameMap.size() < 1) {
            throw new IllegalStateException("dbNameMap size can not small than 1");
        }

        if (dbNameMap.size() != dependencyDbs.size()) {
            throw new IllegalStateException("dbNameMap.size() " + dbNameMap.size() + " != dependencyDbs.size()" + dependencyDbs.size());
        }
        // long timestampp;// = Long.parseLong(ManageUtils.formatNowYyyyMMddHHmmss());
        DataSourceFactoryPluginStore dbPluginStore = null;
        final KoubeiProgressCallback koubeiProgressCallback = new KoubeiProgressCallback();
        List<IbatorContext> daoFacadeList = Lists.newArrayList();
        Long lastOptime = null;
        List<DataSourceFactoryPluginStore> leakFacadeDsPlugin = Lists.newArrayList();
        for (Map.Entry<DBNode, List<String>> /* dbname */
                entry : dbNameMap.entrySet()) {
            dbPluginStore = getFacadePluginStore(entry);
            if (dbPluginStore.getPlugin() == null) {
                leakFacadeDsPlugin.add(dbPluginStore);
            }
        }
        if (leakFacadeDsPlugin.size() > 0) {
            this.msgHandler.addErrorMessage(context, "数据库:"
                    + leakFacadeDsPlugin.stream().map((p) -> "'" + p.getDSKey().keyVal + "'")
                    .collect(Collectors.joining(",")) + "还没有定义对应的Facade数据源");
            return;
        }

        for (Map.Entry<DBNode, List<String>> /* dbname */
                entry : dbNameMap.entrySet()) {
            lastOptime = dependencyDbs.get(entry.getKey().getDbId());
            if (lastOptime == null) {
                throw new IllegalStateException("db " + entry.getKey() + " is not find in dependency dbs:"
                        + dbNameMap.keySet().stream().map((r) -> "[" + r.getDbId() + ":" + r.getDbName() + "]").collect(Collectors.joining(",")));
            }
            long timestamp = lastOptime;
            dbPluginStore = getFacadePluginStore(entry);
            FacadeDataSource facadeDataSource = dbPluginStore.createFacadeDataSource();
            IbatorProperties properties = new IbatorProperties(facadeDataSource, entry.getValue(), timestamp);
            entry.getKey().setTimestampVer(timestamp);
            if (entry.getValue().size() < 1) {
                throw new IllegalStateException("db:" + entry.getKey() + " relevant tablesList can not small than 1");
            }
            KoubeiIbatorRunner runner = new KoubeiIbatorRunner(properties) {

                @Override
                protected KoubeiProgressCallback getProgressCallback() {
                    return koubeiProgressCallback;
                }
            };
            IbatorContext ibatorContext = runner.getIbatorContext();
            daoFacadeList.add(ibatorContext);
            try {
                if (!properties.isDaoScriptCreated()) {
                    // 生成源代码
                    runner.build();
                    // dao script 脚本已经创建不需要再创建了
                    // if (compilerAndPackage) {
                    // 直接生成就行了，别管当前是不是要编译了
                    File classpathDir = new File(Config.getDataDir(), "libs/tis-ibatis");
                    // File classpathDir = new File("/Users/mozhenghua/Desktop/j2ee_solution/project/tis-ibatis/target/dependency");
                    JavaCompilerProcess daoCompilerPackageProcess = new JavaCompilerProcess(facadeDataSource.dbMeta, properties.getDaoDir(), classpathDir);
                    // 打包,生成jar包
                    daoCompilerPackageProcess.compileAndBuildJar();
                    // }
                }
            } catch (Exception e) {
                // 将文件夹清空
                FileUtils.forceDelete(properties.getDaoDir());
                throw new RuntimeException("dao path:" + properties.getDaoDir(), e);
            }
        }
        if (daoFacadeList.size() < 1) {
            throw new IllegalStateException("daoFacadeList can not small than 1");
        }
        daoFacadeList.stream().forEach((r) -> {
            FacadeContext fc = new FacadeContext();
            fc.setFacadeInstanceName(r.getFacadeInstanceName());
            fc.setFullFacadeClassName(r.getFacadeFullClassName());
            fc.setFacadeInterfaceName(r.getFacadeInterface());
            indexStreamCodeGenerator.getFacadeList().add(fc);
        });
        //return dbNameMap;
    }

    private DataSourceFactoryPluginStore getFacadePluginStore(Map.Entry<DBNode, List<String>> entry) {
        DataSourceFactoryPluginStore dbPluginStore;
        dbPluginStore
                = TIS.getDataSourceFactoryPluginStore(new PostedDSProp(DBIdentity.parseId(entry.getKey().getDbName()), DbScope.FACADE));
        return dbPluginStore;
    }

//    private void appendClassFile(File parent, FileObjectsContext fileObjects, final StringBuffer qualifiedClassName) throws IOException {
//        String[] children = parent.list();
//        File childFile = null;
//        for (String child : children) {
//            childFile = new File(parent, child);
//            if (childFile.isDirectory()) {
//                StringBuffer newQualifiedClassName = null;
//                if (qualifiedClassName == null) {
//                    newQualifiedClassName = new StringBuffer(child);
//                } else {
//                    newQualifiedClassName = (new StringBuffer(qualifiedClassName)).append(".").append(child);
//                }
//                appendClassFile(childFile, fileObjects, newQualifiedClassName);
//            } else {
//                final String className = StringUtils.substringBeforeLast(child, ".");
//                //
//                NestClassFileObject fileObj = MyJavaFileManager.getNestClassFileObject(
//                        ((new StringBuffer(qualifiedClassName)).append(".").append(className)).toString(), fileObjects.classMap);
//                try (InputStream input = FileUtils.openInputStream(childFile)) {
//                    IOUtils.copy(input, fileObj.openOutputStream());
//                }
//            }
//        }
//    }
//
//    private boolean streamScriptCompile(File sourceRoot, Set<DBNode> dependencyDBNodes) throws Exception {
//        LogProcessorUtils.LoggerListener loggerListener = new LogProcessorUtils.LoggerListener() {
//
//            @Override
//            public void receiveLog(LogProcessorUtils.Level level, String line) {
//                System.err.println(line);
//            }
//        };
//        return ScalaCompilerSupport.streamScriptCompile(sourceRoot, IDBNodeMeta.appendDBDependenciesClasspath(dependencyDBNodes), loggerListener);
//    }
}
