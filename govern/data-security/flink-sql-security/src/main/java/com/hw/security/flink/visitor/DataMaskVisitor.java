package com.hw.security.flink.visitor;

import com.google.common.collect.ImmutableList;
import com.hw.security.flink.SecurityContext;
import com.hw.security.flink.model.ColumnEntity;
import com.hw.security.flink.enums.DataMaskType;
import com.hw.security.flink.model.TableEntity;
import com.hw.security.flink.visitor.basic.AbstractBasicVisitor;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hw.security.flink.visitor.DataMaskVisitor.ParentType.*;

/**
 * @description: DataMaskVisitor
 * @author: HamaWhite
 */
public class DataMaskVisitor extends AbstractBasicVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(DataMaskVisitor.class);

    public DataMaskVisitor(SecurityContext context, String username) {
        super(context, username);
    }

    @Override
    public Void visit(SqlCall call) {
        if (call instanceof SqlSelect) {
            SqlSelect sqlSelect = (SqlSelect) call;
            if (!isCustomSqlSelect(sqlSelect)) {
                walkTreeMaskTableRef(sqlSelect, SQL_SELECT, sqlSelect.getFrom());
            }
        }
        return super.visit(call);
    }

    private void walkTreeMaskTableRef(SqlNode parent, ParentType parentType, SqlNode from) {
        if (from instanceof SqlJoin) {
            SqlJoin sqlJoin = (SqlJoin) from;
            walkTreeMaskTableRef(sqlJoin, SQL_JOIN_LEFT, sqlJoin.getLeft());
            walkTreeMaskTableRef(sqlJoin, SQL_JOIN_RIGHT, sqlJoin.getRight());
        } else if (from instanceof SqlIdentifier) {
            String tableName = from.toString();
            LOG.debug("SqlIdentifier-tableName: [{}]", tableName);
            // tableAlias is equal to tableName
            addDataMask(parent, parentType, tableName, tableName);
        } else if (from instanceof SqlBasicCall) {
            SqlNode[] operands = ((SqlBasicCall) from).getOperands();
            // for example, for a sub-query, operands[0] is of type SqlSelect
            if (operands[0] instanceof SqlIdentifier) {
                String tableName = operands[0].toString();
                String tableAlias = operands[1].toString();
                LOG.debug("SqlBasicCall-tableName: [{}], tableAlias: [{}]", tableName, tableAlias);
                addDataMask(parent, parentType, tableName, tableAlias);
            }
        }
    }

    private void addDataMask(SqlNode parent, ParentType parentType, String tableName, String tableAlias) {
        TableEntity table = securityContext.getTable(tableName);
        boolean doColumnMasking = false;
        List<String> columnTransformerList = new ArrayList<>();
        for (ColumnEntity column : table.getColumnList()) {
            String columnTransformer = column.getColumnName();
            Optional<String> condition = policyManager.getDataMaskCondition(username
                    , securityContext.getCurrentCatalog()
                    , securityContext.getCurrentDatabase()
                    , tableName
                    , column.getColumnName());
            if (condition.isPresent()) {
                doColumnMasking = true;
                DataMaskType maskType = policyManager.getDataMaskType(condition.get());
                columnTransformer = maskType.getTransformer().replace("{col}", column.getColumnName());
            }
            columnTransformerList.add(columnTransformer);
        }
        if (doColumnMasking) {
            String replaceText = buildReplaceText(table, columnTransformerList);
            SqlSelect sqlSelect = (SqlSelect) securityContext.parseExpression(replaceText);
            // mark this SqlSelect as custom, no need to rewrite
            setSqlSelectCustom(sqlSelect, true);
            SqlNode[] operands = new SqlNode[2];
            operands[0] = sqlSelect;
            // add table alias
            operands[1] = new SqlIdentifier(ImmutableList.of(tableAlias)
                    , null
                    , new SqlParserPos(0, 0)
                    , null
            );
            SqlBasicCall replaced = new SqlBasicCall(new SqlAsOperator()
                    , operands
                    , new SqlParserPos(0, 0)
            );
            rewrittenTree(parent, parentType, replaced);
        }
    }

    private String buildReplaceText(TableEntity table, List<String> columnTransformerList) {
        StringBuilder sb = new StringBuilder();
        sb.append("(SELECT ");
        boolean firstOne = true;
        for (int index = 0; index < columnTransformerList.size(); index++) {
            String transformer = columnTransformerList.get(index);
            if (!firstOne) {
                sb.append(", ");
            } else {
                firstOne = false;
            }
            ColumnEntity column = table.getColumnList().get(index);
            String colName = column.getColumnName();
            if (!transformer.equals(colName)) {
                // CAST(transformer AS col_type) AS col_name
                sb.append(String.format("CAST( %s AS %s) AS %s", transformer, column.getColumnType(), column.getColumnName()));
            } else {
                sb.append(column.getColumnName());
            }
        }
        sb.append(" FROM ");
        sb.append(table.getTableName());
        sb.append(")");
        return sb.toString();
    }

    private void rewrittenTree(SqlNode parent, ParentType parentType, SqlBasicCall replaced) {
        switch (parentType) {
            case SQL_SELECT:
                ((SqlSelect) parent).setFrom(replaced);
                break;
            case SQL_JOIN_LEFT:
                ((SqlJoin) parent).setLeft(replaced);
                break;
            case SQL_JOIN_RIGHT:
                ((SqlJoin) parent).setRight(replaced);
                break;
            default:
                throw new IllegalArgumentException("Unsupported parent type: " + parentType);
        }
    }

    public enum ParentType {
        // parent is SqlSelect
        SQL_SELECT,
        // parent is the left of SqlJoin
        SQL_JOIN_LEFT,
        // parent is the right of SqlJoin
        SQL_JOIN_RIGHT
    }

    public void setSqlSelectCustom(SqlSelect sqlSelect, boolean custom) {
        try {
            Method declaredMethod = SqlSelect.class.getDeclaredMethod("setCustom", boolean.class);
            declaredMethod.invoke(sqlSelect, custom);
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    public boolean isCustomSqlSelect(SqlSelect sqlSelect) {
        try {
            Method declaredMethod = SqlSelect.class.getDeclaredMethod("isCustom");
            return (boolean) declaredMethod.invoke(sqlSelect);
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }
}



