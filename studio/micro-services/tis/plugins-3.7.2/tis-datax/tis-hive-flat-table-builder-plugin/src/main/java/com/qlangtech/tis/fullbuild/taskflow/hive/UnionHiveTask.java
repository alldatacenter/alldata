///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.fullbuild.taskflow.hive;
//
//import com.qlangtech.tis.fs.IFs2Table;
//import com.qlangtech.tis.fs.ITISFileSystem;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus.JoinTaskStatus;
//import com.qlangtech.tis.hive.HiveColumn;
//import com.qlangtech.tis.hive.HiveInsertFromSelectParser;
//import com.qlangtech.tis.order.center.IJoinTaskContext;
//import com.qlangtech.tis.order.center.IParamContext;
//import com.qlangtech.tis.plugin.datax.MREngine;
//import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
//import com.qlangtech.tis.sql.parser.er.ERRules;
//import org.antlr.runtime.tree.Tree;
//import org.apache.commons.lang.StringUtils;
//import org.apache.hadoop.hive.ql.parse.HiveParser;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///* *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public class UnionHiveTask extends JoinHiveTask {
//
//    private String partition;
//
//    private Set<String> columnSet = new LinkedHashSet<>();
//
//    private Set<String> partitionColumns = new HashSet<>();
//
//    private List<String> subTaskSqls = new ArrayList<>();
//
//    private List<HiveInsertFromSelectParser> parserList = new ArrayList<>();
//
//    private static final String RECORD_NAME = "record";
//
//    private static final Logger logger = LoggerFactory.getLogger(UnionHiveTask.class);
//
//    public UnionHiveTask(SqlTaskNodeMeta nodeMeta, boolean isFinalNode, ERRules erRules
//            , JoinTaskStatus joinTaskStatus, ITISFileSystem fileSystem, IFs2Table fs2Table, MREngine mrEngine) {
//        super(nodeMeta, isFinalNode, erRules, joinTaskStatus, fileSystem, fs2Table, mrEngine);
//    }
//
//    // public UnionHiveTask(JoinTaskStatus joinTaskStatus) {
//    // super(joinTaskStatus);
//    // }
//    private String tableName;
//
//    public String getPartition() {
//        return partition;
//    }
//
//    public String getTableName() {
//        return this.tableName;
//    }
//
//    public void setTableName(String tableName) {
//        this.tableName = tableName;
//    }
//
//    public void setPartition(String partition) {
//        this.partition = partition;
//        if (!StringUtils.isBlank(partition)) {
//            Arrays.stream(partition.split(",")).map(String::trim).forEach(s -> partitionColumns.add(s));
//        }
//    }
//
//    private List<String> getSubTaskSqls() {
//        return subTaskSqls;
//    }
//
//    void setSubTaskSqls(List<String> subTaskSqls) {
//        this.subTaskSqls = subTaskSqls;
//        parseSubTab();
//    }
//
//    @Override
//    public String getContent() {
//        return super.getContent();
//    }
//
//    private void parseSubTab() {
//        IJoinTaskContext chainContext = getContext().getExecContext();
//        for (String subTaskSql : getSubTaskSqls()) {
//            HiveInsertFromSelectParser parser = new HiveInsertFromSelectParser();
//            // try {
//            // StringWriter writer = new StringWriter();
//            // velocityEngine.evaluate(velocityContext, writer, "sql", subTaskSql);
//            // subTaskSql = writer.toString();
//            // IOUtils.close(writer);
//            parser.start(subTaskSql);
//            parserList.add(parser);
//            parser.getCols().stream().filter(column -> !partitionColumns.contains(column.getName())).forEach(column -> columnSet.add(column.getName()));
//            // } catch (IOException | ParseException e) {
//            // throw new IllegalStateException("parse sub table " + e.getMessage(), e);
//            // }
//        }
//        columnSet.addAll(partitionColumns);
//        // FIXME: in order to pass the compile phase ,make setContent comment
//        // setContent(getUnionSql());
//        chainContext.setAttribute(IParamContext.KEY_BUILD_TARGET_TABLE_NAME, this.getTableName());
//        chainContext.setAttribute("colsExcludePartitionColsList", columnSet.stream().filter(column -> !partitionColumns.contains(column)).collect(Collectors.toList()));
//    }
//
//    private String getUnionSql() {
//        StringBuilder sb = new StringBuilder();
//        sb.append("INSERT OVERWRITE TABLE ").append(getTableName());
//        if (!StringUtils.isBlank(partition)) {
//            sb.append(" PARTITION (").append(partition).append(")");
//        }
//        sb.append("\n");
//        Set<String> columnSet = getColumnSet();
//        sb.append("SELECT ").append(getSetString(columnSet)).append(" FROM (\n");
//        sb.append(getParsersString()).append(") AS ").append(RECORD_NAME);
//        return sb.toString();
//    }
//
//    private Set<String> getColumnSet() {
//        return columnSet;
//    }
//
//    private String getParsersString() {
//        StringBuilder sb = new StringBuilder();
//        int parserSize = parserList.size();
//        int parserCnt = 0;
//        for (HiveInsertFromSelectParser parser : parserList) {
//            Map<String, HiveColumn> columnMap = parser.getColsMap();
//            sb.append("SELECT ");
//            int columnSize = columnSet.size();
//            int columnCnt = 0;
//            for (String column : columnSet) {
//                if (columnMap.containsKey(column)) {
//                    HiveColumn hiveColumn = columnMap.get(column);
//                    if (hiveColumn.hasAliasName()) {
//                        sb.append(hiveColumn.getRawName()).append(" AS ").append(column);
//                    } else if (hiveColumn.hasDefaultValue()) {
//                        sb.append(hiveColumn.getDefalutValue()).append(" AS ").append(column);
//                    } else {
//                        sb.append(hiveColumn.getName());
//                    }
//                } else {
//                    sb.append("'' AS ").append(column);
//                }
//                if (++columnCnt < columnSize) {
//                    sb.append(", ");
//                }
//            }
//            sb.append(" FROM `").append(parser.getSourceTableName()).append("`");
//            if (parser.getWhere() != null) {
//                sb.append(" where ").append(getConditionString(parser.getWhere().getChild(0)));
//            }
//            if (++parserCnt < parserSize) {
//                sb.append("\nUNION ALL\n");
//            }
//        }
//        return sb.toString();
//    }
//
//    private static String getSetString(Set<String> set) {
//        StringBuilder sb = new StringBuilder();
//        int size = set.size();
//        int cnt = 0;
//        for (String column : set) {
//            sb.append(RECORD_NAME).append(".").append(column);
//            if (++cnt < size) {
//                sb.append(", ");
//            }
//        }
//        return sb.toString();
//    }
//
//    private static String getConditionString(Tree root) {
//        if (root.getChildCount() == 2) {
//            if (root.getType() == HiveParser.TOK_FUNCTION) {
//                return String.format("%s(%s)", getConditionString(root.getChild(0)), getConditionString(root.getChild(1)));
//            } else {
//                return String.format("%s %s %s", getConditionString(root.getChild(0)), root.getText(), getConditionString(root.getChild(1)));
//            }
//        } else if (root.getChildCount() == 1 && HiveParser.TOK_TABLE_OR_COL == root.getType()) {
//            return HiveInsertFromSelectParser.getTokTableOrTypeString(root);
//        } else if (root.getChildCount() == 0) {
//            return root.getText();
//        } else {
//            return "";
//        }
//    }
//
//    public static void main(String[] args) throws Exception {
//        // TaskConfigParser parse = TaskConfigParser.getInstance();
//        // UnionHiveTask unionTask =
//        // parse.getUnionHiveTask("search4supplyUnionTabs");
//        // System.out.println(unionTask.getUnionSql());
//        // HiveInsertFromSelectParser unionParser =
//        // unionTask.getSQLParserResult(new TemplateContext(null));
//        // List<HiveColumn> columns = unionParser.getColsExcludePartitionCols();
//        // String blank = " ";
//        // for (HiveColumn c : unionParser.getCols()) {
//        //
//        // System.out.println("<field name=\"" + c.getName() + "\" "
//        // + StringUtils.substring(blank, 0, 20 -
//        // StringUtils.length(c.getName()))
//        // + " type=\"string\" stored=\"true\" indexed=\"false\" />");
//        // }
//    }
//}
