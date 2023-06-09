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
package com.qlangtech.tis.sql.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.facebook.presto.sql.parser.ParsingException;
import com.facebook.presto.sql.parser.ParsingOptions;
import com.facebook.presto.sql.parser.SqlParser;
import com.facebook.presto.sql.tree.Expression;
import com.facebook.presto.sql.tree.NodeLocation;
import com.facebook.presto.sql.tree.Statement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.order.center.IAppSourcePipelineController;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.sql.parser.er.ERRules;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.exception.TisSqlFormatException;
import com.qlangtech.tis.sql.parser.meta.ColumnTransfer;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.meta.Position;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import com.qlangtech.tis.sql.parser.utils.DefaultDumpNodeMapContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 对应脚本配置的反序列化对象类
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年7月29日
 */
public class SqlTaskNodeMeta implements ISqlTask {

    public static final ThreadLocal<Yaml> yaml = new ThreadLocal<Yaml>() {
        protected Yaml initialValue() {
            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
            dumperOptions.setIndent(4);
            dumperOptions.setDefaultScalarStyle(ScalarStyle.PLAIN);
            dumperOptions.setAnchorGenerator((n) -> "a");
            // dumperOptions.setAnchorGenerator(null);
            dumperOptions.setPrettyFlow(false);
            dumperOptions.setSplitLines(true);
            dumperOptions.setLineBreak(LineBreak.UNIX);
            dumperOptions.setWidth(1000000);
            Yaml y = new Yaml(new Constructor(), new Representer() {

                @Override
                protected Node representScalar(Tag tag, String value, ScalarStyle style) {
                    // 大文本实用block
                    if (Tag.STR == tag && value.length() > 100) {
                        style = ScalarStyle.FOLDED;
                    }
                    return super.representScalar(tag, value, style);
                }

                @Override
                protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                    if (propertyValue == null) {
                        return null;
                    }
                    if (DependencyNode.class.equals(property.getType())) {
                        return null;
                    } else {
                        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                    }
                }
            }, dumperOptions);
            y.addTypeDescription(new TypeDescription(DependencyNode.class, Tag.MAP, DependencyNode.class));
            y.addTypeDescription(new TypeDescription(SqlTaskNodeMeta.class, Tag.MAP, SqlTaskNodeMeta.class));
            y.addTypeDescription(new TypeDescription(DumpNodes.class, Tag.MAP, DumpNodes.class));
            return y;
        }
    };

    private static final String FILE_NAME_DEPENDENCY_TABS = "dependency_tabs.yaml";

    private static final String FILE_NAME_PROFILE = "profile.json";

    public static final String KEY_PROFILE_TIMESTAMP = "timestamp";

    public static final String KEY_PROFILE_TOPOLOGY = "topology";

    public static final String KEY_PROFILE_ID = "id";

    private static final SqlParser sqlParser = new com.facebook.presto.sql.parser.SqlParser();


    /**
     * 对sql进行粗略的校验
     *
     * @param sql
     * @param dependencyNodes
     * @return
     */
    public static Optional<TisSqlFormatException> validateSql(String sql, List<DependencyNode> dependencyNodes) {
        //Optional<TisSqlFormatException> result  = Optional.empty();
        SqlTaskNodeMeta taskNodeMeta = new SqlTaskNodeMeta();
        // 这个sql语句有错误，需要校验成错误，抛异常
        taskNodeMeta.setSql(sql);

        final IJoinTaskContext tskContext = new DftJoinTaskContext(ExecutePhaseRange.fullRange());


        try {
            String pt = "20200703113848";
            ITabPartition p = () -> pt;
            Map<IDumpTable, ITabPartition> tabPartition = dependencyNodes.stream().collect(Collectors.toMap((r) -> EntityName.parse(r.getName()), (r) -> p));

            taskNodeMeta.getRewriteSql("testTaskName", new MockDumpPartition(tabPartition), () -> new IPrimaryTabFinder() {
            }, tskContext, false);
            return Optional.empty();
        } catch (Throwable e) {
            int indexOf;
            if ((indexOf = ExceptionUtils.indexOfType(e, TisSqlFormatException.class)) > -1) {
                TisSqlFormatException ex = (TisSqlFormatException) ExceptionUtils.getThrowables(e)[indexOf];
                //System.out.println(ex.summary());
                return Optional.of(ex);
                //  assertEquals("base ref:gg can not find relevant table entity in map,mapSize:1,exist:[g:tis.commodity_goods],位置，行:1,列:44", ex.summary());
            } else {
                throw e;
            }
        }

    }


    private static class MockDumpPartition extends TabPartitions {


        public MockDumpPartition(Map<IDumpTable, ITabPartition> tabPartition) {
            super(tabPartition);
        }

        @Override
        public int size() {
            return 2;
        }

//        protected Optional<Map.Entry<IDumpTable, ITabPartition>> findTablePartition(boolean dbNameCriteria, String dbName, String tableName) {
//            return Optional.of(new EntryPair(EntityName.parse(dbNameCriteria ? dbName : IDumpTable.DEFAULT_DATABASE_NAME + "." + tableName), () -> pt));
//        }

    }

//    private static class EntryPair implements Map.Entry<IDumpTable, ITabPartition> {
//        private final IDumpTable key;
//        private final ITabPartition val;
//
//        public EntryPair(IDumpTable key, ITabPartition val) {
//            this.key = key;
//            this.val = val;
//        }
//
//        @Override
//        public IDumpTable getKey() {
//            return key;
//        }
//
//        @Override
//        public ITabPartition getValue() {
//            return val;
//        }
//
//        @Override
//        public ITabPartition setValue(ITabPartition value) {
//            return null;
//        }
//    }

    /**
     * 为了在创建表之前先取得创建表的Col Type需要，需要将用户提交的SQL 进行rewriter将所有的pt的约束都改成无效pt，这样查询会立即进行
     *
     * @return
     */
    public RewriteSql getColMetaGetterSql(TabPartitions tabPartitions) {

        IJoinTaskContext joinContext = getTplContext();
        Optional<List<Expression>> parameters = Optional.empty();

        SqlStringBuilder builder = new SqlStringBuilder();
        SelectColsMetaGetter rewriter = new SelectColsMetaGetter(builder, () -> new IPrimaryTabFinder() {
        }, tabPartitions, parameters, joinContext);
        // 执行rewrite
        try {
            Statement state = getSqlStatement();
            rewriter.process(state, new  FormatContext(0 ) );
        } catch (TisSqlFormatException e) {
            throw e;
        } catch (Exception e) {
            // String dp = dumpPartition.toString(); //dumpPartition.entrySet().stream().map((ee) -> "[" + ee.getKey() + "->" + ee.getValue().getPt() + "]").collect(Collectors.joining(","));
            throw new IllegalStateException(e);
        }
        return new RewriteSql(this.getSql(), builder.toString(), rewriter.outputCols, null);
    }

    private IJoinTaskContext getTplContext() {
        final IJoinTaskContext taskContext = new IJoinTaskContext() {
            @Override
            public String getIndexName() {
                return null;
            }

            @Override
            public boolean isDryRun() {
                return false;
            }

            @Override
            public boolean hasIndexName() {
                return false;
            }

            @Override
            public int getTaskId() {
                return 0;
            }

            @Override
            public int getIndexShardCount() {
                return 1;
            }

            @Override
            public <T> T getAttribute(String key) {
                return null;
            }

            @Override
            public void setAttribute(String key, Object v) {

            }

            @Override
            public <T> T getAttribute(String key, Supplier<T> creator) {
                return null;
            }

            @Override
            public IAppSourcePipelineController getPipelineController() {
                return null;
            }

            @Override
            public ExecutePhaseRange getExecutePhaseRange() {
                return ExecutePhaseRange.fullRange();
            }

            @Override
            public String getString(String key) {
                return null;
            }

            @Override
            public boolean getBoolean(String key) {
                return false;
            }

            @Override
            public int getInt(String key) {
                return 0;
            }

            @Override
            public long getLong(String key) {
                return 0;
            }

            @Override
            public long getPartitionTimestampWithMillis() {
                throw new UnsupportedOperationException();
            }
        };
        return taskContext;
    }

    @Override
    public RewriteSql getRewriteSql(String taskName, TabPartitions dumpPartition
            , Supplier<IPrimaryTabFinder> erRules, IJoinTaskContext joinContext, boolean isFinalNode) {
        if (dumpPartition.size() < 1) {
            throw new IllegalStateException("taskName:" + taskName + " dumpPartition set size can not small than 1");
        }
        Optional<List<Expression>> parameters = Optional.empty();

        SqlStringBuilder builder = new SqlStringBuilder();
        SqlRewriter rewriter = new SqlRewriter(builder, dumpPartition, erRules, parameters, isFinalNode, joinContext);
        // 执行rewrite
        try {
            Statement state = getSqlStatement();
            rewriter.process(state, new FormatContext(0));
        } catch (TisSqlFormatException e) {
            throw e;
        } catch (Exception e) {
            String dp = dumpPartition.toString();
            throw new IllegalStateException("task:" + taskName + ",isfinalNode:" + isFinalNode + ",dump tabs pt:" + dp + "\n" + e.getMessage(), e);
        }
        SqlRewriter.AliasTable primaryTable = rewriter.getPrimayTable();
        if (primaryTable == null) {
            throw new IllegalStateException("task:" + taskName + " has not find primary table");
        }
        return new RewriteSql(this.getSql(), builder.toString(), rewriter.outputCols, rewriter.getPrimayTable());
    }

    private Statement getSqlStatement() {
        final String sql = this.getSql();
        try {
            return sqlParser.createStatement(sql, new ParsingOptions());
        } catch (ParsingException e) {
            throw new TisSqlFormatException(e.getErrorMessage(), Optional.of(new NodeLocation(e.getLineNumber(), e.getColumnNumber())));
        }
    }

    /**
     * Content用于保存到yaml内容中 <br>
     * 保证每行开头为空格 <br>
     * 保证文本最后部位空格<br>
     *
     * @param content
     * @return
     */
    public static String processBigContent(String content) {
        LineIterator lIt = null;
        String line = null;
        StringBuffer result = new StringBuffer();
        // final boolean firstLine = true;
        try (StringReader reader = new StringReader(processFileContent(content))) {
            lIt = IOUtils.lineIterator(reader);
            while (lIt.hasNext()) {
                line = lIt.next();
                if (!StringUtils.startsWith(line, " ")) {
                    result.append(" ");
                }
                result.append(line).append("\n");
                // firstLine = false;
            }
        }
        return StringUtils.trimToEmpty(result.toString());
    }

    private static String processFileContent(String content) {
        return content.replace("\r\n", "\n");
    }

    /**
     * 将对象持久化
     *
     * @param topology
     */
    public static void persistence(SqlDataFlowTopology topology, File parent) throws Exception {
        if (!parent.exists()) {
            throw new IllegalStateException("parent not exist:" + parent.getAbsolutePath());
        }
        if (topology.profile == null || StringUtils.isEmpty(topology.getName()) || topology.getTimestamp() < 1 || topology.getDataflowId() < 1) {
            throw new IllegalArgumentException("param topology's prop name timestamp or dataflowid neither can be null");
        }
        Pattern PatternjoinNode = Pattern.compile("[\\da-z]+\\-[\\da-z]+\\-[\\da-z]+\\-[\\da-z]+\\-[\\da-z]+\\.yaml");
        // 用来处理被删除的节点，如果某个节点被删除的话对应的AtomicBoolean flag 就为false
        Map<String, AtomicBoolean> oldNodeFileStats
                = Arrays.stream(parent.list((dir, name) -> PatternjoinNode.matcher(name).matches()))
                .collect(Collectors.toMap((filename) -> filename, (filename) -> new AtomicBoolean()));

        String nodeFileName = null;
        AtomicBoolean hasProcess = null;
        for (SqlTaskNodeMeta process : topology.getNodeMetas()) {
            if (StringUtils.isEmpty(process.getId())) {
                throw new IllegalStateException(process.getExportName() + " relevant node property id can not be null ");
            }
            nodeFileName = (process.getId() + ".yaml");
            hasProcess = oldNodeFileStats.get(nodeFileName);
            if (hasProcess != null) {
                hasProcess.set(true);
            }
            try (OutputStreamWriter output = new OutputStreamWriter(FileUtils.openOutputStream(new File(parent, nodeFileName)))) {
                yaml.get().dump(process, output);
            }
        }
        oldNodeFileStats.entrySet().forEach((e) -> {
            // 老文件 没有被处理 说明已经被删除了
            if (!e.getValue().get()) {
                FileUtils.deleteQuietly(new File(parent, e.getKey()));
            }
        });
        try (OutputStreamWriter output = new OutputStreamWriter(FileUtils.openOutputStream(new File(parent, FILE_NAME_DEPENDENCY_TABS)))) {
            yaml.get().dump(new DumpNodes(topology.getDumpNodes()), output);
        }
        try (OutputStreamWriter output = new OutputStreamWriter(FileUtils.openOutputStream(new File(parent, FILE_NAME_PROFILE)), TisUTF8.get())) {
            JSONObject profile = new JSONObject();
            profile.put(KEY_PROFILE_TIMESTAMP, topology.getTimestamp());
            profile.put(KEY_PROFILE_TOPOLOGY, topology.getName());
            profile.put(KEY_PROFILE_ID, topology.getDataflowId());
            IOUtils.write(profile.toJSONString(), output);
        }

    }

    @SuppressWarnings("all")
    public static SqlDataFlowTopology getSqlDataFlowTopology(String topologyName) throws Exception {
        SqlDataFlowTopology result = getSqlDataFlowTopology(getTopologyDir(topologyName));
        ERRules.getErRule(topologyName);
        return result;
    }

    public static TopologyDir getTopologyDir(String topologyName) {
        File wfDir = SqlTaskNode.parent;
        wfDir = new File(wfDir, topologyName);
        try {
            FileUtils.forceMkdir(wfDir);
        } catch (IOException e) {
            throw new RuntimeException("wfDir:" + wfDir.getAbsolutePath(), e);
        }
        return new //
                TopologyDir(wfDir, topologyName);
    }

    public static TopologyProfile getTopologyProfile(String topologyName) throws Exception {
        TopologyDir topologyDir = getTopologyDir(topologyName);
        return getTopologyProfile(topologyDir.synchronizeRemoteRes(FILE_NAME_PROFILE));
    }

    @SuppressWarnings("all")
    public static SqlDataFlowTopology getSqlDataFlowTopology(TopologyDir topologyDir) throws Exception {
        SqlDataFlowTopology topology = new SqlDataFlowTopology();
        List<File> subFiles = topologyDir.synchronizeSubRemoteRes();
        if (subFiles.size() < 1) {
            throw new IllegalStateException("subFiles size can not small than 1,file:" + topologyDir.dir);
        }

        File dependencyTabFile = new File(topologyDir.dir, FILE_NAME_DEPENDENCY_TABS);
        if (!dependencyTabFile.exists()) {
            return topology;
        }

        try {
            // dump节点
            try (Reader reader = new InputStreamReader(FileUtils.openInputStream(dependencyTabFile), TisUTF8.get())) {
                DumpNodes dumpTabs = yaml.get().loadAs(reader, DumpNodes.class);
                // topology.set
                topology.addDumpTab(dumpTabs.getDumps());
            }
        } catch (Exception e) {
            throw new RuntimeException(dependencyTabFile.getAbsolutePath(), e);
        }
        Iterator<File> fit = FileUtils.iterateFiles(topologyDir.dir, new String[]{"yaml"}, false);
        File next = null;
        while (fit.hasNext()) {
            next = fit.next();
            if (ERRules.ER_RULES_FILE_NAME.equals(next.getName()) || FILE_NAME_DEPENDENCY_TABS.equals(next.getName())) {
                continue;
            }
            SqlTaskNodeMeta sqlTaskNodeMeta = deserializeTaskNode(next);
            topology.addNodeMeta(sqlTaskNodeMeta);
        }
        // 设置profile内容信息
        topology.setProfile(getTopologyProfile(new File(topologyDir.dir, FILE_NAME_PROFILE)));
        return topology;

    }

    /**
     * 取得topology的基本信息
     *
     * @param profileFile
     * @return
     * @throws Exception
     */
    public static TopologyProfile getTopologyProfile(File profileFile) throws Exception {
        if (!profileFile.exists()) {
            throw new IllegalStateException("profile not exist:" + profileFile.getAbsolutePath());
        }
        // 设置profile内容信息
        try (InputStream r = FileUtils.openInputStream(profileFile)) {
            JSONObject j = JSON.parseObject(IOUtils.toString(r, TisUTF8.get()));
            TopologyProfile profile = new TopologyProfile();
            profile.setDataflowId(j.getLong(KEY_PROFILE_ID));
            profile.setName(j.getString(KEY_PROFILE_TOPOLOGY));
            profile.setTimestamp(j.getLong(KEY_PROFILE_TIMESTAMP));
            return profile;
        }
    }

    public static SqlTaskNodeMeta deserializeTaskNode(File file) {
        try {
            try (Reader scriptReader = new InputStreamReader(FileUtils.openInputStream(file), TisUTF8.get())) {
                return deserializeTaskNode(scriptReader);
            }
        } catch (Exception e) {
            throw new RuntimeException(file.getAbsolutePath(), e);
        }
    }

    public static SqlTaskNodeMeta deserializeTaskNode(Reader scriptReader) throws Exception {

        SqlTaskNodeMeta sqlTaskNodeMeta = null;
        sqlTaskNodeMeta = yaml.get().loadAs(scriptReader, SqlTaskNodeMeta.class);
        return sqlTaskNodeMeta;

    }

    public static class TopologyProfile {

        private long timestamp;

        private String name;

        private long dataflowId;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getDataflowId() {
            return dataflowId;
        }

        public void setDataflowId(long dataflowId) {
            this.dataflowId = dataflowId;
        }
    }

    public static class SqlDataFlowTopology {

        private TopologyProfile profile = new TopologyProfile();

        //join处理节点，后续hive，spark处理
        private List<SqlTaskNodeMeta> nodeMetas = Lists.newArrayList();
        //数据源节点
        private List<DependencyNode> dumpNodes = Lists.newArrayList();

        public static SqlDataFlowTopology deserialize(String jsonContent) {
            return JSON.parseObject(jsonContent, SqlDataFlowTopology.class);
        }

        /**
         * 是否是单节点处理模式，整个工作流就一个表，所以后续hive，就不需要处理了
         */
        public boolean isSingleTableModel() {
            return isSingleDumpTableDependency() && nodeMetas.size() < 1;
        }


        /**
         * 是否是单表数据导入方式
         *
         * @return
         */
        public boolean isSingleDumpTableDependency() {
            return this.dumpNodes.size() < 2;
        }

        @JSONField(serialize = false)
        public long getTimestamp() {
            return profile.getTimestamp();
        }

        @JSONField(serialize = false)
        public String getName() {
            if (this.profile == null) {
                throw new IllegalStateException("profile can not be null");
            }
            return this.profile.getName();
        }

        @JSONField(serialize = false)
        public long getDataflowId() {
            return profile.dataflowId;
        }

        public void setProfile(TopologyProfile profile) {
            this.profile = profile;
        }

        public TopologyProfile getProfile() {
            return this.profile;
        }

        // ///////////////////////////////////////
        // ================================================================
        //
        private Map<String, DependencyNode> dumpNodesMap;

        @JSONField(serialize = false)
        public Map<String, /*** table name */
                DependencyNode> getDumpNodesMap() {
            if (this.dumpNodesMap == null) {
                this.dumpNodesMap = Maps.newHashMap();
                List<DependencyNode> dumpNodes = this.getDumpNodes();
                dumpNodes.stream().forEach((r) -> {
                    this.dumpNodesMap.put(r.getName(), r);
                    r.setExtraSql(null);
                });
            }
            return this.dumpNodesMap;
        }

        private DAGSessionSpec sessionSpec;

        @JSONField(serialize = false)
        public DAGSessionSpec getDAGSessionSpec() {
            if (this.sessionSpec != null) {
                return this.sessionSpec;
            }
            this.sessionSpec = new DAGSessionSpec();
            // join = null;
            //  StringBuffer dagSessionSpec = new StringBuffer();
            // ->a ->b a,b->c
            for (DependencyNode dump : this.getDumpNodes()) {
                //    dagSessionSpec.append("->").append(dump.getId()).append(" ");
                sessionSpec.getDpt(dump.getId());
            }
            for (SqlTaskNodeMeta pnode : this.getNodeMetas()) {
                DAGSessionSpec join = sessionSpec.getDpt(pnode.getId());
                pnode.getDependencies().forEach((node) -> {
                    join.addDpt(sessionSpec.getDpt(node.getId()));
                });
            }
            return sessionSpec;
            //  return dagSessionSpec.toString();
        }


        /**
         * 取得dataflow中的表依赖关系
         *
         * @return
         */
        private Map<EntityName, List<TableTupleCreator>> createDumpNodesMap() {
            final Map<EntityName, List<TableTupleCreator>> result = Maps.newHashMap();
            this.dumpNodes.stream().forEach((node) -> {
                List<TableTupleCreator> tables = null;
                TableTupleCreator tupleCreator = null;
                EntityName entityName = EntityName.parse(node.getDbName() + "." + node.getName());
                tupleCreator = new TableTupleCreator(entityName.toString(), NodeType.DUMP);
                tables = result.get(entityName);
                if (tables == null) {
                    tables = Lists.newArrayList();
                    result.put(entityName, tables);
                }
                tupleCreator.setRealEntityName(entityName);
                tables.add(tupleCreator);
            });
            return result;
        }

        private List<SqlTaskNode> allNodes = null;

        public List<SqlTaskNode> parseTaskNodes() throws Exception {
            if (this.allNodes == null) {
                final DefaultDumpNodeMapContext dumpNodsContext = new DefaultDumpNodeMapContext(this.createDumpNodesMap());
                this.allNodes = this.getNodeMetas().stream().map((m) -> {
                    SqlTaskNode node = new SqlTaskNode(EntityName.parse(m.getExportName()), m.getNodeType(), dumpNodsContext);
                    node.setContent(m.getSql());
                    return node;
                }).collect(Collectors.toList());
                dumpNodsContext.setAllJoinNodes(allNodes);
            }
            return this.allNodes;
        }

        public TableTupleCreator parseFinalSqlTaskNode() throws Exception {
            SqlTaskNode task = getFinalTaskNode();
            return task.parse(true);
        }

        @JSONField(serialize = false)
        public List<ColumnMetaData> getFinalTaskNodeCols() throws Exception {

            if (this.isSingleTableModel()) {
                DependencyNode dumpNode = this.getDumpNodes().get(0);
                DataSourceFactory dbPlugin = TIS.getDataBasePlugin(new PostedDSProp(DBIdentity.parseId(dumpNode.getDbName())));
                List<ColumnMetaData> cols = dbPlugin.getTableMetadata(false, dumpNode.parseEntityName());
                return cols; //tisTable.getReflectCols();
//                        .stream().map((c) -> {
//                    return new ColName(c.getKey());
//                }).collect(Collectors.toList());
            }

            SqlTaskNode task = this.getFinalTaskNode();
            List<ColName> colNames = task.parse(false).getColsRefs().getColRefMap().keySet();
            AtomicInteger index = new AtomicInteger();
            return colNames.stream().map((c) -> {
                return new ColumnMetaData(index.getAndIncrement(), c.getAliasName(), new DataType(Types.VARCHAR), false);
            }).collect(Collectors.toList());
        }

        private SqlTaskNode getFinalTaskNode() throws Exception {

            if (isSingleTableModel()) {
                final DefaultDumpNodeMapContext dumpNodsContext = new DefaultDumpNodeMapContext(this.createDumpNodesMap());
                DependencyNode dumpNode = getFirstDumpNode();
                SqlTaskNode taskNode = new SqlTaskNode(EntityName.create(dumpNode.getDbName(), dumpNode.getName()), NodeType.JOINER_SQL, dumpNodsContext);
                DataSourceFactory dbPlugin = TIS.getDataBasePlugin(new PostedDSProp(DBIdentity.parseId(dumpNode.getDbName())));
                // List<ColumnMetaData> tableMetadata = ;
                //TISTable tab = dbPlugin.loadTableMeta(dumpNode.getName());
                taskNode.setContent(ColumnMetaData.buildExtractSQL(dumpNode.getName(), true, dbPlugin.getTableMetadata(false,dumpNode.parseEntityName())).toString());
                return taskNode;
            } else {

                List<SqlTaskNode> taskNodes = this.parseTaskNodes();
                SqlTaskNode task = null;
                final String finalNodeName = this.getFinalNode().getExportName();
                //
                Optional<SqlTaskNode> f = //
                        taskNodes.stream().filter((n) -> finalNodeName.equals(n.getExportName().getTabName())).findFirst();
                if (!f.isPresent()) {
                    String setStr = taskNodes.stream().map((n) -> n.getExportName().getJavaEntityName()).collect(Collectors.joining(","));
                    throw new IllegalStateException("finalNodeName:" + finalNodeName + " can not find node in[" + setStr + "]");
                }
                /**
                 * *******************************
                 * 开始解析
                 * *******************************
                 */
                task = f.get();
                return task;
            }
        }

        public DependencyNode getFirstDumpNode() {
            return null;
//            Optional<DependencyNode> singleDumpNode = this.getDumpNodes().stream().findFirst();
//            if (!singleDumpNode.isPresent()) {
//                throw new IllegalStateException(this.getName() + " has not set dump node");
//            }
//            return singleDumpNode.get();
        }


        /////////////////////////////////////////

        public SqlDataFlowTopology() {
            super();
        }

        public void addNodeMeta(SqlTaskNodeMeta nodeMeta) {
            this.nodeMetas.add(nodeMeta);
        }

        public List<SqlTaskNodeMeta> getNodeMetas() {
            return this.nodeMetas;
        }

        public void addDumpTab(List<DependencyNode> ns) {
            this.dumpNodes.addAll(ns);
        }

        public void addDumpTab(DependencyNode ns) {
            this.dumpNodes.add(ns);
        }

        public void setNodeMetas(List<SqlTaskNodeMeta> nodeMetas) {
            this.nodeMetas = nodeMetas;
        }

        public void setDumpNodes(List<DependencyNode> dumpNodes) {
            this.dumpNodes = dumpNodes;
        }

        public List<DependencyNode> getDumpNodes() {
            return this.dumpNodes;
        }

        @JSONField(serialize = false)
        public SqlTaskNodeMeta getFinalNode() throws Exception {
            // if (this.isSingleTableModel()) {
//                Optional<DependencyNode> singleDumpNode = this.getDumpNodes().stream().findFirst();
//                if (!singleDumpNode.isPresent()) {
//                    throw new IllegalStateException(this.getName() + " has not set dump node");
//                }
//                SqlTaskNodeMeta nodeMeta = new SqlTaskNodeMeta();
//                DependencyNode dumpNode = singleDumpNode.get();
//                nodeMeta.setType(NodeType.DUMP.getType());
//                nodeMeta.setSql(dumpNode.getExtraSql());
//                nodeMeta.setExportName(EntityName.create(dumpNode.getDbName(), dumpNode.getName()).toString());
//                return nodeMeta;
            //  } else {
            Collection<SqlTaskNodeMeta> finalNodes = getFinalNodes().values();
            if (finalNodes.size() != 1) {
                throw new IllegalStateException(//
                        "finalNodes size must be 1,but now is:" + finalNodes.size() + ",nodes:[" + //
                                finalNodes.stream().map((r) -> r.getExportName()).collect(Collectors.joining(",")) + "]");
            }
            Optional<SqlTaskNodeMeta> taskNode = finalNodes.stream().findFirst();
            if (!taskNode.isPresent()) {
                throw new IllegalStateException("final node shall be exist");
            }
            return taskNode.get();
            //}
        }


        /**
         * 取dataflow的最终的输出节点(没有下游节点的节点)
         *
         * @return
         * @throws Exception
         */
        public Map<String, SqlTaskNodeMeta> getFinalNodes() {
            Map<String, RefCountTaskNode> /*export Name*/
                    exportNameRefs = Maps.newHashMap();
            for (SqlTaskNodeMeta meta : getNodeMetas()) {
                exportNameRefs.put(meta.getId(), new RefCountTaskNode(meta));
            }
            RefCountTaskNode refCount = null;
            for (SqlTaskNodeMeta meta : getNodeMetas()) {
                for (DependencyNode entry : meta.getDependencies()) {
                    refCount = exportNameRefs.get(entry.getId());
                    if (refCount == null) {
                        continue;
                    }
                    refCount.incr();
                }
            }
            //
            Map<String, SqlTaskNodeMeta> finalNodes = //
                    exportNameRefs.values().stream().filter(//
                            (e) -> e.refCount.get() < 1).map((r) -> r.taskNode).collect(Collectors.toMap((n) -> n.getExportName(), (n) -> n));
            return finalNodes;
        }
    }

    private static class RefCountTaskNode {

        private final AtomicInteger refCount = new AtomicInteger();

        private final SqlTaskNodeMeta taskNode;

        public RefCountTaskNode(SqlTaskNodeMeta taskNode) {
            this.taskNode = taskNode;
        }

        public void incr() {
            this.refCount.incrementAndGet();
        }
    }

    private String id;

    private String exportName;

    private String type;

    private Position position;

    private String sql;

    private List<DependencyNode> dependencies = Lists.newArrayList();

    public List<DependencyNode> getDependencies() {
        return this.dependencies;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDependencies(List<DependencyNode> required) {
        this.dependencies = required;
    }

    public void addDependency(DependencyNode required) {
        this.dependencies.add(required);
    }

    @Override
    public String getSql() {
        return this.sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    @Override
    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getType() {
        return this.type;
    }

    @JSONField(serialize = false)
    public NodeType getNodeType() {
        return NodeType.parse(this.type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public static class HiveColTransfer {

        public static HiveColTransfer instance = new HiveColTransfer();

        public String transfer(String base, String field, ColumnTransfer transfer) {
            try {
                Method method = HiveColTransfer.class.getMethod(transfer.getTransfer(), String.class, String.class, ColumnTransfer.class);
                return (String) method.invoke(instance, base, field, transfer);
            } catch (Exception e) {
                throw new RuntimeException("base:" + base + ",field:" + field + "," + transfer.toString(), e);
            }
        }

        // regexp_replace(tp.curr_date,'-',''),
        // select tp.curr_date, from_unixtime(int(tp.op_time/1000), 'yyyyMMddHHmmss') as op_time
        // ,from_unixtime(int(tp.operate_date/1000), 'yyyyMMddHHmmss') as operate_date
        // , int(tp.load_time+'0000') as int1
        // , int(tp.load_time) as int2
        // ,from_unixtime(int(tp.load_time+'0000'), 'yyyyMMddHHmmss') as load_time
        // ,from_unixtime(int(tp.load_time), 'yyyyMMddHHmmss') as load_time2
        // ,from_unixtime(int(tp.modify_time), 'yyyyMMddHHmmss') as modify_time
        // from ods_order_compare_out.totalpayinfo as tp limit 10
        public String dateYYYYmmdd(String base, String field, ColumnTransfer transfer) {
            final String param = getParam(base, field, transfer);
            return "regexp_replace(" + param + ",'-','') as " + transfer.getColKey();
        }

        public String dateYYYYMMddHHmmss(String base, String field, ColumnTransfer transfer) {
            final String param = getParam(base, field, transfer);
            return "from_unixtime(int(" + param + "), 'yyyyMMddHHmmss') as " + transfer.getColKey();
        }

        private String getParam(String base, String field, ColumnTransfer transfer) {
            return StringUtils.replace(transfer.getParam(), "value", base + "." + field);
        }
    }

    private static class DftJoinTaskContext implements IJoinTaskContext {
        private final ExecutePhaseRange executePhaseRange;

        @Override
        public boolean isDryRun() {
            return false;
        }

        public DftJoinTaskContext(ExecutePhaseRange executePhaseRange) {
            this.executePhaseRange = executePhaseRange;
        }

        @Override
        public String getIndexName() {
            return null;
        }

        @Override
        public boolean hasIndexName() {
            return false;
        }

        @Override
        public int getTaskId() {
            return 0;
        }

        @Override
        public int getIndexShardCount() {
            return 0;
        }

        @Override
        public <T> T getAttribute(String key) {
            return null;
        }

        @Override
        public void setAttribute(String key, Object v) {

        }

        @Override
        public <T> T getAttribute(String key, Supplier<T> creator) {
            return null;
        }

        @Override
        public IAppSourcePipelineController getPipelineController() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ExecutePhaseRange getExecutePhaseRange() {
            return executePhaseRange;
        }

        @Override
        public String getString(String key) {
            return null;
        }

        @Override
        public boolean getBoolean(String key) {
            return false;
        }

        @Override
        public int getInt(String key) {
            return 0;
        }

        @Override
        public long getLong(String key) {
            return 0;
        }

        @Override
        public long getPartitionTimestampWithMillis() {
            throw new UnsupportedOperationException();
        }
    }

//    /**
//     * @author: 百岁（baisui@qlangtech.com）
//     * @create: 2023-03-02 13:39
//     **/
//    public static class DependNode {
//        private final String id;
//        private final String name;
//
//        public DependNode(String id, String name) {
//            if (org.apache.commons.lang3.StringUtils.isEmpty(id)) {
//                throw new IllegalArgumentException("param id can not be null");
//            }
//            if (org.apache.commons.lang3.StringUtils.isEmpty(name)) {
//                throw new IllegalArgumentException("param name can not be null");
//            }
//            this.id = id;
//            this.name = name;
//        }
//
//        public String getId() {
//            return this.id;
//        }
//
//        public String getName() {
//            return this.name;
//        }
//    }
}
