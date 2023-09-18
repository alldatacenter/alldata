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
package com.qlangtech.tis.hive;

import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.sql.parser.ISqlTask;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.Context;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.HiveParser;
import org.apache.hadoop.hive.ql.parse.ParseDriver;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 宽表解析成AST之后的遍历语意树之后生成的语义模型
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年12月22日 下午6:27:06
 */
public class HiveInsertFromSelectParser extends AbstractInsertFromSelectParser {


    private final Map<String, HiveColumn> colsMap = new HashMap<>();

    private int colIndex = 0;

    private String targetTableName;

    private String sourceTableName;

//    public static final List<String> preservedPsCols;
//
//    static {
//        preservedPsCols = Collections.unmodifiableList(Lists.newArrayList(IDumpTable.PARTITION_PMOD, IDumpTable.PARTITION_PT));
//    }

    private ASTNode where;

    private static final ParseDriver parseDriver;

    private static final Context parseContext;

    static {
        try {
            parseDriver = new ParseDriver();
            Configuration config = new Configuration();
            config.set("hive.support.sql11.reserved.keywords", "false");
            config.set("_hive.hdfs.session.path", "/user");
            config.set("_hive.local.session.path", "/user");
            parseContext = new Context(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HiveInsertFromSelectParser(String sql, Function<ISqlTask.RewriteSql, List<ColumnMetaData>> sqlColMetaGetter) {
        super(sql, sqlColMetaGetter);
    }

//    public HiveInsertFromSelectParser() {
//        super(true);
//    }

    public String getTargetTableName() {
        return targetTableName;
    }

    private void setTargetTableName(String targetTableName) {
        this.targetTableName = targetTableName;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public Map<String, HiveColumn> getColsMap() {
        return colsMap;
    }

    public ASTNode getWhere() {
        return where;
    }

    public void setWhere(ASTNode where) {
        this.where = where;
    }


//    @Override
//    protected void parseCreateTable(String sql) {
//        try {
//            ASTNode astNode = parseDriver.parse(sql, HiveInsertFromSelectParser.parseContext);
//            parseCreateTable(astNode);
//        } catch (ParseException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private void parseCreateTable(Node node) {
//        ASTNode astNode;
//        for (Node n : node.getChildren()) {
//            astNode = ((ASTNode) n);
//            if (HiveParser.TOK_QUERY == astNode.getType()) {
//                parseCreateTable(astNode);
//            }
//            // 20170201
//            if (HiveParser.TOK_FROM == astNode.getType()) {
//                parseSourceTable(astNode, false);
//            }
//            if (HiveParser.TOK_INSERT == astNode.getType()) {
//                parseCreateTable(astNode);
//            }
//            if (HiveParser.TOK_DESTINATION == astNode.getType()) {
//                parseTargetTable(astNode, false, false);
//            }
//            if (HiveParser.TOK_SELECT == astNode.getType()) {
//                parseCreateTable(astNode);
//            }
//            if (HiveParser.TOK_SELEXPR == astNode.getType()) {
//                parseColumn(astNode);
//            }
//            // 20170201
//            if (HiveParser.TOK_WHERE == astNode.getType()) {
//                where = astNode;
//            }
//        }
//    }

//    private void parseSourceTable(ASTNode node, boolean isTableName) {
//        for (Node n : node.getChildren()) {
//            ASTNode astNode = (ASTNode) n;
//            if (HiveParser.TOK_TABREF == astNode.getType()) {
//                parseSourceTable(astNode, false);
//                continue;
//            }
//            if (HiveParser.TOK_TABNAME == astNode.getType()) {
//                parseSourceTable(astNode, true);
//                setSourceTableName(StringUtils.join(astNode.getChildren().stream().filter(node1 -> {
//                    ASTNode astNode1 = (ASTNode) node1;
//                    return astNode1.getType() == HiveParser.Identifier;
//                }).toArray(), "."));
//                break;
//            }
//        }
//    }

//    private void parseTargetTable(ASTNode node, boolean isTableName, boolean isPartition) {
//        ASTNode astNode;
//        for (Node n : node.getChildren()) {
//            astNode = (ASTNode) n;
//            if (HiveParser.TOK_TAB == astNode.getType()) {
//                parseTargetTable(astNode, false, false);
//                continue;
//            }
//            if (HiveParser.TOK_TABNAME == astNode.getType()) {
//                // add by baisui:20170704 支持数据库名称
//                // if (astNode.getChildCount() > 1) {
//                setTargetTableName(StringUtils.join(astNode.getChildren().stream().filter(node1 -> {
//                    ASTNode astNode1 = (ASTNode) node1;
//                    return astNode1.getType() == HiveParser.Identifier;
//                }).toArray(), "."));
//                continue;
//            }
//            if (HiveParser.TOK_PARTSPEC == astNode.getType()) {
//                parseTargetTable(astNode, false, false);
//                continue;
//            }
//            if (HiveParser.TOK_PARTVAL == astNode.getType()) {
//                parseTargetTable(astNode, false, true);
//                continue;
//            }
//            if (isPartition) {
//                //ps.add(astNode.getText());
//            }
//            if (isTableName) {
//                setTargetTableName(astNode.getText());
//            }
//        }
//    }
//
//    /**
//     * colume有三种状态 '' AS id id1 AS id id record.id
//     */
//    private void parseColumn(ASTNode astNode) {
//        HiveColumn column = new HiveColumn();
//        for (Node n : astNode.getChildren()) {
//            ASTNode node = (ASTNode) n;
//            if (HiveParser.Identifier == node.getType()) {
//                column.setName(node.getText());
//            } else if (HiveParser.TOK_TABLE_OR_COL == node.getType()) {
//                column.setRawName(getTokTableOrTypeString(node));
//            } else if (HiveParser.StringLiteral == node.getType()) {
//                column.setDefalutValue(node.getText());
//            } else if (HiveParser.DOT == node.getType()) {
//                column.setRawName(node.getChild(1).getText());
//            }
//        }
//        column.setIndex(colIndex++);
//        // column.setType(SupportHiveDataType.STRING.name());
//        column.setDataType(DataType.createVarChar(256));
//        this.getCols().add(column);
//        this.colsMap.put(column.getName(), column);
//    }

//    public static String getTokTableOrTypeString(Tree node) {
//        assert (HiveParser.TOK_TABLE_OR_COL == node.getType());
//        return node.getChild(0).getText();
//    }

    public static void main(String[] args) throws Exception {
        // AbstractInsertFromSelectParser parse = new HiveInsertFromSelectParser();
        // parse.start("INSERT OVERWRITE TABLE totalpay_summary PARTITION (pt,pmod)\n"
        // + " SELECT
        // tp.totalpay_id,tp.curr_date,tp.outfee,tp.source_amount,tp.discount_amount,tp"
        // + ".coupon_discount\n"
        // + " ,tp.result_amount,tp.recieve_amount,tp.ratio,tp.status,tp.entity_id\n"
        // + " ,tp.is_valid,tp.op_time,tp.last_ver,tp.op_user_id,tp.discount_plan_id\n"
        // + " ,tp.operator,tp.operate_date,tp.card_id,tp.card,tp.card_entity_id\n"
        // + "
        // ,tp.is_full_ratio,tp.is_minconsume_ratio,tp.is_servicefee_ratio,tp.invoice_code\n"
        // + "
        // ,tp.invoice_memo,tp.invoice,tp.over_status,tp.is_hide,tp.load_time,tp.modify_time\n"
        // + " ,o.order_id,o.seat_id,o.area_id,o.is_valido
        // ,o.instance_count,o.all_menu,o.all_fee,o"
        // + ".people_count \n" + " ,o.order_from \n" + " ,o.order_kind\n"
        // + " ,o.inner_code\n" + " ,o.open_time\n" + " ,o.end_time\n"
        // + " ,o.order_status\n" + " ,o.code,o.seat_code\n" + " ,p.kindpay\n"
        // + " ,sp.special_fee_summary\n"
        // + " ,cc.code as card_code,cc.inner_code as card_inner_code,cc.customer_id as
        // card_customer_id\n"
        // + " ,cc.name as card_customer_name,cc.spell AS card_customer_spell\n"
        // + " ,cc.mobile AS card_customer_moble,cc.phone AS card_customer_phone\n"
        // + " ,tp.pt,tp.pmod\n"
        // + " FROM totalpay tp INNER JOIN order_instance o ON (tp.totalpay_id =
        // o.totalpay_id and length(o"
        // + ".all_menu)<10000)\n"
        // + " LEFT JOIN tmp_pay p ON (tp.totalpay_id = p.totalpay_id)\n"
        // + " LEFT JOIN tmp_group_specialfee sp ON( tp.totalpay_id = sp.totalpay_id
        // )\n"
        // + " LEFT JOIN tmp_customer_card AS cc on(tp.card_id = cc.id AND tp.entity_id=
        // cc"
        // + ".entity_id)");
//        parse.start(FileUtils.readFileToString(new File("insertsql.txt"), "utf8"), Collections.emptyMap());
//        System.out.println("getTargetTableName:" + parse.getTargetTableName());
//        List<HiveColumn> columns = parse.getColsExcludePartitionCols();
//        int i = 1;
    }


    /**
     * 重建WHERE條件
     *
     * @return
     */
    public StringBuffer rebuildWhere() {
        ASTNode where = this.getWhere();
        if (where != null && HiveParser.TOK_WHERE == where.getType()) {
            StringBuffer wbuffer = new StringBuffer("WHERE ");
            for (int i = 0; i < where.getChildCount(); i++) {
                rebuildWhere(wbuffer, (ASTNode) where.getChild(i));
            }
            // System.out.println(wbuffer.toString());
            return wbuffer;
        }
        return null;
    }

    private static void rebuildWhere(StringBuffer buffer, ASTNode where) {
        if (HiveParser.TOK_TABLE_OR_COL == where.getType()) {
            rebuildWhere(buffer, (ASTNode) where.getChild(0));
            return;
        }
        int childCount = where.getChildCount();
        if (childCount > 0) {
            rebuildWhere(buffer, (ASTNode) where.getChild(0));
            buffer.append(" ").append(where.getText());
            if (childCount > 1) {
                rebuildWhere(buffer, (ASTNode) where.getChild(1));
            }
        } else {
            buffer.append(" ").append(where.getText());
        }
    }
}
