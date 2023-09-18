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
package com.qlangtech.tis.sql.parser;

import com.facebook.presto.sql.parser.ParsingOptions;
import com.facebook.presto.sql.parser.SqlParser;
import com.facebook.presto.sql.tree.Query;
import com.facebook.presto.sql.tree.TISStackableAstVisitor.StackableAstVisitorContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreatorVisitor;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.ColRef;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.FunctionDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import com.qlangtech.tis.sql.parser.visitor.StreamTransformVisitor;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sql执行节点
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年4月19日
 */
public class SqlTaskNode {

    public static File parent = new File(Config.getMetaCfgDir(), IFullBuildContext.NAME_DATAFLOW_DIR);

    // private Map<String, SqlTaskNode> all;
    private Optional<TisGroupBy> groupBy = Optional.empty();

    private final IDumpNodeMapContext dumpNodesContext;

    // 依赖的数据库表，可能有相同的表明，但是来自两个数据库的情况
    // private static final Map<String /* tableName */, List<TableTupleCreator>> dumpNodes;
    // static {
    // try {
    //
    // List<TableTupleCreator> tables = null;
    // Map<String /* tableName */, List<TableTupleCreator>> builder = Maps.newHashMap();
    //
    // File f = new File(
    // "D:\\j2ee_solution\\eclipse-java-oxygen-mars-develop\\workspace\\tis-mars\\tis-sql-parser\\src\\main\\resources\\dump_tabs\\dump_tabs.txt");
    // LineIterator lineIt = FileUtils.lineIterator(f, "utf8");
    // String line = null;
    // TableTupleCreator tupleCreator = null;
    //
    // EntityName entityName = null;
    // while (lineIt.hasNext()) {
    // line = lineIt.nextLine();
    //
    // entityName = EntityName.parse(line);
    //
    // tupleCreator = new TableTupleCreator(line, NodeType.DUMP);
    //
    // tables = builder.get(entityName.getTabName());
    // if (tables == null) {
    // tables = Lists.newArrayList();
    // builder.put(entityName.getTabName(), tables);
    // }
    //
    // tupleCreator.setRealEntityName(entityName);
    // tables.add(tupleCreator);
    //
    // }
    // dumpNodes = Collections.unmodifiableMap(builder);// builder.build();
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    //
    // }
    public static List<ColumnMetaData> reflectTableCols(String sql) {
        if (StringUtils.isEmpty(sql)) {
            throw new IllegalArgumentException("param sql can not be null");
        }
        List<ColumnMetaData> result = Lists.newArrayList();
        Query query = parseQuery(sql);
        StreamTransformVisitor v = new StreamTransformVisitor(null);
        query.accept(v, new StackableAstVisitorContext<>(1));
        ColRef colsRef = v.getColsRef();
        int index = 0;
        for (Map.Entry<ColName, IDataTupleCreator> /* colName */
                entry : colsRef.getColRefMap().entrySet()) {
            // int index, String key, int type, boolean pk
            result.add(new ColumnMetaData(index++, StringUtils.lowerCase(entry.getKey().getName())
                    , new DataType(-1)
                    , /**  * 暂时无法取到类型，先用-1占一下位置    */
                    false));
        }
        return result;
    }

    public static final SqlParser parser = new SqlParser();

    // private static final TestGroupBySqlParser sqlParser = new
    // TestGroupBySqlParser();
    // private Rewriter rewriter;
    // private ColRef colsRefs = null;
    private String content;

    // 依赖的数据
    private final Map<EntityName, SqlTaskNode> required = Maps.newHashMap();

    private final TaskNodeRecognizeVisitor taskNodeRecognizedVisitor;

    // 名称
    private final EntityName exportName;

    private final NodeType nodetype;

    public SqlTaskNode(EntityName exportName, NodeType nodetype, IDumpNodeMapContext dumpNodesContext) {
        super();
        this.exportName = exportName;
        this.nodetype = nodetype;
        this.dumpNodesContext = dumpNodesContext;
        this.taskNodeRecognizedVisitor = new TaskNodeRecognizeVisitor(dumpNodesContext);
    }

    public EntityName getExportName() {
        return exportName;
    }

    public NodeType getNodetype() {
        return nodetype;
    }

    public void addRequired(String node) {
        if (StringUtils.isBlank(node)) {
            throw new IllegalArgumentException("param node can not be null");
        }
        EntityName ename = EntityName.parse(node);
        this.dumpNodesContext.accurateMatch(ename.getTabName());
        // List<TableTupleCreator> tabs = dumpNodes.get(ename.getTabName());
        // if (tabs != null && tabs.size() > 1) {
        // throw new IllegalStateException("table:" + ename.getTabName()
        // + " has more than 1 match dumptable,shall special full tablename[dbanme.tablename]");
        // }
        this.required.put(ename, null);
    }

    public void addRequired(EntityName node, SqlTaskNode taskNode) {
        if (node == null) {
            throw new IllegalArgumentException("param node can not be null");
        }
        if (taskNode == null) {
            throw new IllegalArgumentException("param taskNode can not be null");
        }
        this.required.put(node, taskNode);
    }

    public static Query parseQuery(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new NullPointerException("param sql can not be null");
        }
        Query query = (Query) parser.createStatement(sql, new ParsingOptions());
        return query;
    }

    private TableTupleCreator tupleCterator = null;


    public TableTupleCreator parse(boolean parseAllRefTab) {
        if (tupleCterator != null) {
            return this.tupleCterator;
        }
        this.tupleCterator = new TableTupleCreator(this.exportName.getTabName(), nodetype);
        tupleCterator.setRealEntityName(this.exportName);
        try {
            Query query = parseQuery(this.getContent());
            StreamTransformVisitor v = new StreamTransformVisitor(this.dumpNodesContext);
            query.accept(v, new StackableAstVisitorContext<>(1));
            ColRef colsRefs = v.getColsRef();
            tupleCterator.setColsRefs(colsRefs);
            // TaskNode 識別
            if (parseAllRefTab) {
                for (Map.Entry<String, IDataTupleCreator> /*** ref */entry : colsRefs.getBaseRefEntities()) {
                    entry.getValue().accept(taskNodeRecognizedVisitor);
                }
            }
            return tupleCterator;
        } catch (Exception e) {
            throw new RuntimeException("exportName:" + this.exportName, e);
        }
    }

    public Set<Map.Entry<EntityName, SqlTaskNode>> getRequired() {
        return required.entrySet();
    }

    public String getRequiredTabsSetLiteria() {
        return this.getRequired().stream().map(e -> String.valueOf(e.getKey())).collect(Collectors.joining(","));
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Optional<TisGroupBy> getGroupBy() {
        return this.groupBy;
    }

    private static class TaskNodeRecognizeVisitor implements IDataTupleCreatorVisitor {

        private final IDumpNodeMapContext dumpNodesContext;

        public TaskNodeRecognizeVisitor(IDumpNodeMapContext dumpNodesContext) {
            this.dumpNodesContext = dumpNodesContext;
        }

        @Override
        public void visit(FunctionDataTupleCreator function) {
            for (IDataTupleCreator tupleCreator : function.getParams().values()) {
                tupleCreator.accept(this);
            }
        }

        @Override
        public void visit(TableTupleCreator tableTuple) {
            try {
                ColRef colRef = tableTuple.getColsRefs();
                if (colRef == null && tableTuple.getNodetype() == NodeType.JOINER_SQL) {
                    SqlTaskNode taskNode = dumpNodesContext.geTaskNode(tableTuple.getEntityName());
                    // SqlTaskNode taskNode = SqlTaskNode.geTaskNode(tableTuple.getEntityName().getTabName());
                    tableTuple.setColsRefs(taskNode.parse(true).getColsRefs());
                    taskNode.getGroupBy();
                    colRef = tableTuple.getColsRefs();
                    colRef.getBaseRefEntities().forEach((entry) -> {
                        entry.getValue().accept(TaskNodeRecognizeVisitor.this);
                    });
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toString() {
        return "SqlTaskNode:" + this.getExportName();
    }

    // private static final Pattern PATTERN_EXPORT =
    // Pattern.compile("export:(.+?)");
    // private static final Pattern PATTERN_REQUIRED =
    // Pattern.compile("required:(.+?)");
    public static void main(String[] args) throws Exception {
        // parseTaskNode();
    }

    // public static List<SqlTaskNode> parseTaskNodes(SqlDataFlowTopology topology) throws Exception {
    // List<SqlTaskNode> allNodes = null;
    //
    // final DefaultDumpNodeMapContext dumpNodsContext = new DefaultDumpNodeMapContext(topology.createDumpNodesMap());
    //
    // allNodes = topology.getNodeMetas().stream().map((m) -> {
    // SqlTaskNode node = new SqlTaskNode(EntityName.parse(m.getExportName()), m.getNodeType(), dumpNodsContext);
    // node.setContent(m.getSql());
    // return node;
    // }).collect(Collectors.toList());
    // dumpNodsContext.setAllJoinNodes(allNodes);
    // return allNodes;
    // }
    // private static SqlTaskNode parseTaskNode(SqlTaskNodeMeta sqlNodeMeta) throws Exception {
    // final String exportName = sqlNodeMeta.getExportName();
    // List<String> requires = sqlNodeMeta.getDependencies().stream().map((r) -> r.getName())
    // .collect(Collectors.toList());
    // if (requires.size() < 1) {
    // throw new IllegalStateException("name:" + sqlNodeMeta.getExportName() + "id:" + sqlNodeMeta.getId()
    // + " ,required is illegal, size can not small than 1");
    // }
    // SqlTaskNode taskNode = new SqlTaskNode(EntityName.parse(exportName), sqlNodeMeta.getNodeType());
    // taskNode.setContent(sqlNodeMeta.getSql());
    // for (String r : requires) {
    // taskNode.addRequired(r);
    // }
    //
    // return taskNode;
    // }
    // 打印自己並且遍歷子節點
    // public void visit(TaskNodeTraversesCreatorVisitor taskNodeVisitor) {
    //
    // for (Map.Entry<ColName /* colName */, IDataTupleCreator> centry :
    // this.getColsRefs().colRefMap.entrySet()) {
    //
    // // PropGetter peek = taskNodeVisitor.propStack.isEmpty() ? null :
    // // taskNodeVisitor.propStack.peek();
    //
    // // if (peek == null || StringUtils.equals(peek.getOutput().getName(),
    // // centry.getKey().getAliasName())) {
    //
    // taskNodeVisitor.pushPropGetter(centry.getKey(), this.getExportName(),
    // centry.getValue());
    // // } else {
    // // taskNodeVisitor.pushPropGetter(centry.getKey(), this, centry.getValue());
    // // }
    // try {
    // centry.getValue().accept(taskNodeVisitor);
    // } finally {
    // taskNodeVisitor.propStack.pop();
    // }
    // // if (peek != null) {
    // // break;
    // // }
    // }
    //
    // }
    // private static void visitColsRefs(TaskNodeTraversesCreatorVisitor
    // taskNodeVisitor, SqlTaskNode taskNode) {
    //
    // }
    // public static SqlTaskNode findTerminatorTaskNode(List<SqlTaskNode> taskNodes) {
    // SqlTaskNode terminatorNode = null;
    // aa:
    // for (SqlTaskNode taskNode : taskNodes) {
    //
    // if (taskNode.getRequired().isEmpty()) {
    // continue;
    // }
    //
    // for (SqlTaskNode t : taskNodes) {
    //
    // if (t.getRequired().stream().filter((e) -> {
    // // return (StringUtils.equals(taskNode.getExportName().getTabName(),
    // // e.getKey()));
    //
    // return taskNode.getExportName().equals(e.getKey());// (StringUtils.equals(.getTabName(),
    // // e.getKey()));
    // }).count() > 0) {
    // // 有被依赖到
    // continue aa;
    // }
    // }
    //
    // terminatorNode = taskNode;
    // break;
    // }
    //
    // if (terminatorNode == null) {
    // throw new IllegalStateException("can not find terminator task node");
    // }
    // return terminatorNode;
    // }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SqlTaskNode)) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return this.exportName.hashCode();
    }
}
