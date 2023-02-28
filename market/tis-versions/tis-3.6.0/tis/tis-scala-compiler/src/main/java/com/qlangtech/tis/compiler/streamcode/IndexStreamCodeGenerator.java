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

import com.google.common.collect.Lists;
import com.qlangtech.tis.compiler.java.FileObjectsContext;
import com.qlangtech.tis.compiler.java.ResourcesFile;
import com.qlangtech.tis.compiler.java.ZipPath;
import com.qlangtech.tis.manage.IBasicAppSource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.manage.common.incr.StreamContextConstant;
import com.qlangtech.tis.sql.parser.DBNode;
import com.qlangtech.tis.sql.parser.stream.generate.FacadeContext;
import com.qlangtech.tis.sql.parser.stream.generate.StreamComponentCodeGeneratorFlink;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import javax.tools.JavaFileObject;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class IndexStreamCodeGenerator {
    public final String collection;

    // private SqlTaskNodeMeta.SqlDataFlowTopology dfTopology;
    private final IBasicAppSource streamIncrGenerateStrategy;
    public final long incrScriptTimestamp;

    private List<FacadeContext> facadeList;

    private StreamComponentCodeGeneratorFlink streamCodeGenerator;

    private File streamScriptRootDir;

    private final IDBTableNamesGetter dbTableNamesGetter;

    // private String workflowName;
    // 自动生成的incr脚本中需要dao支持吗？


    public IndexStreamCodeGenerator(String collection, IBasicAppSource streamIncrGenerateStrategy, long incrScriptTimestamp
            , IDBTableNamesGetter dbTableNamesGetter) throws Exception {
        if (StringUtils.isEmpty(collection)) {
            throw new IllegalArgumentException("argument collection can not be null");
        }

        this.collection = collection;
        this.streamIncrGenerateStrategy = streamIncrGenerateStrategy;
        this.dbTableNamesGetter = dbTableNamesGetter;
        if (incrScriptTimestamp < 1) {
            throw new IllegalArgumentException("illegal incrScriptTimestamp can not small than 1");
        }
        // 增量脚本时间戳
        // ManageUtils.formatNowYyyyMMddHHmmss(latestOptime);
        this.incrScriptTimestamp = incrScriptTimestamp;
        this.initialize();
    }

    /**
     * 删除生成的脚本
     */
    public void deleteScript() {
        FileUtils.deleteQuietly(streamScriptRootDir);
    }

    private void initialize() throws Exception {
        // FullbuildWorkflowAction.getDataflowTopology(CoreAction.this, this.workFlow);
        // this.dfTopology = SqlTaskNodeMeta.getSqlDataFlowTopology(this.workflowName);
        //  this.dbTables = getDependencyTables(dfTopology);
        facadeList = Lists.newArrayList();
        streamCodeGenerator = new StreamComponentCodeGeneratorFlink(
                this.collection, incrScriptTimestamp, facadeList, this.streamIncrGenerateStrategy);
        this.streamScriptRootDir = StreamContextConstant.getStreamScriptRootDir(this.collection, incrScriptTimestamp);
    }

//    private Map<DBNode, List<String>> /* tables */
//            dbTables;

    public Map<DBNode, List<String>> getDbTables() {
        // return this.streamIncrGenerateStrategy.getDependencyTables(this.dbTableNamesGetter);
        return Collections.emptyMap();
    }

    public boolean isIncrScriptDirCreated() {
        return this.streamCodeGenerator.isIncrScriptDirCreated();
    }

    public String getIncrScriptDirPath() {
        return this.streamCodeGenerator.getIncrScriptDir().getAbsolutePath();
    }


//    public SqlTaskNodeMeta.SqlDataFlowTopology getDfTopology() {
//        return dfTopology;
//    }

    public long getIncrScriptTimestamp() {
        return this.incrScriptTimestamp;
    }

    public List<FacadeContext> getFacadeList() {
        return this.facadeList;
    }

    public void generateConfigFiles() throws Exception {
        this.streamCodeGenerator.generateConfigFiles();
    }

    public FileObjectsContext getSpringXmlConfigsObjectsContext() {
        FileObjectsContext xmlConfigs = new FileObjectsContext();
        Stack<String> childPath = new Stack<>();
        File parent = streamCodeGenerator.getSpringConfigFilesDir();
        if (!parent.exists()) {
            throw new IllegalStateException("file:" + parent.getAbsolutePath() + " is not exist");
        }
        FileObjectsContext.traversingFiles(childPath, parent, xmlConfigs, (zp, child) -> {
            ZipPath zipPath = new ZipPath(zp, child.getName(), JavaFileObject.Kind.OTHER);
            ResourcesFile res = new ResourcesFile(zipPath, FileUtils.readFileToByteArray(child));
            xmlConfigs.resources.add(res);
        });
        return xmlConfigs;
    }

    /**
     * 读出增量scala脚本中的内容
     *
     * @return
     * @throws Exception
     */
    public String readIncrScriptMainFileContent() throws Exception {
        File incrScript = this.streamCodeGenerator.getIncrScriptMainFile();
        if (!incrScript.exists()) {
            throw new IllegalStateException("incrScript:" + incrScript.getAbsolutePath() + " is not exist");
        }
        return FileUtils.readFileToString(incrScript, TisUTF8.getName());
    }

    public StreamComponentCodeGeneratorFlink getStreamCodeGenerator() {
        return this.streamCodeGenerator;
    }

    public void generateStreamScriptCode() throws Exception {
        // 生成scala代码
        this.streamCodeGenerator.build();
        // 生成spring配置文件
        // this.streamCodeGenerator.generateConfigFiles(this.mqConfigMetas);
    }

    // public void saveDbDependencyMetaConfig() throws Exception {
//        streamIncrGenerateStrategy.getDependencyTables(this.dbTableNamesGetter);
//        this.dbTables.keySet();
    // }

    // public File getMqConfigMetaFile() {
    // return new File(this.streamScriptRootDir, "meta/mq_config.yaml");
    // }
    // /**
    // * db 依赖版本配置依赖元数据
    // * @return
    // */
    // public File getDbDependencyConfigMetaFile() {
    // return new File(this.streamScriptRootDir
    // , StreamContextConstant.DIR_META + "/" + StreamContextConstant.FILE_DB_DEPENDENCY_CONFIG);
    // }

//    /**
//     * 取得依赖的db->table映射关系
//     */
//    private Map<DBNode, /** dbname */List<String>> getDependencyTables(SqlTaskNodeMeta.SqlDataFlowTopology dfTopology) throws Exception {
//        Map<DBNode, List<String>> /* tables */dbNameMap = Maps.newHashMap();
//        List<String> tables = null;
//        DBNode dbNode = null;
//        for (DependencyNode node : dfTopology.getDumpNodes()) {
//            dbNode = new DBNode(node.getDbName(), Integer.parseInt(node.getDbid()));
//            node.parseEntityName();
//            tables = dbNameMap.get(dbNode);
//            if (tables == null) {
//                // DB 下的全部table
//                tables = Lists.newArrayList();
//                dbNameMap.put(dbNode, tables);
//            }
//            tables.add(node.getName());
//        }
//        for (Map.Entry<DBNode, List<String>> /* tables */
//                entry : dbNameMap.entrySet()) {
//            entry.setValue(dbTableNamesGetter.getTableNames(entry.getKey().getDbId(), entry.getValue()));
//        }
//        return dbNameMap;
//    }


}
