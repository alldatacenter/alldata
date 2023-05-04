package com.hw.security.flink;

import com.hw.security.flink.model.ColumnEntity;
import com.hw.security.flink.model.TableEntity;
import com.hw.security.flink.visitor.DataMaskVisitor;
import com.hw.security.flink.visitor.RowFilterVisitor;
import javassist.*;
import org.apache.calcite.sql.SqlNode;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.Schema.UnresolvedColumn;
import org.apache.flink.table.api.Schema.UnresolvedComputedColumn;
import org.apache.flink.table.api.Schema.UnresolvedMetadataColumn;
import org.apache.flink.table.api.Schema.UnresolvedPhysicalColumn;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.internal.TableEnvironmentImpl;
import org.apache.flink.table.catalog.AbstractCatalog;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.planner.delegation.ParserImpl;
import org.apache.flink.types.Row;
import org.apache.flink.util.FlinkRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * @description: SecurityContext
 * @author: HamaWhite
 */
public class SecurityContext {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityContext.class);

    private TableEnvironmentImpl tableEnv;

    private final ParserImpl parser;

    private final PolicyManager policyManager;

    static {
        /*
          Use javassist to modify the bytecode to add the variable custom to org.apache.calcite.sql.SqlSelect,
          which is used to mark whether SqlSelect is custom generated
         */
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.getCtClass("org.apache.calcite.sql.SqlSelect");

            // add field custom, the default value is false
            CtField field = new CtField(CtClass.booleanType, "custom", ctClass);
            ctClass.addField(field, CtField.Initializer.constant(false));
            // add set method
            CtMethod setMethod = CtNewMethod.setter("setCustom", field);
            ctClass.addMethod(setMethod);
            // add get method
            CtMethod getMethod = CtNewMethod.getter("isCustom", field);
            ctClass.addMethod(getMethod);
            // load class
            ctClass.toClass();
        } catch (Exception e) {
            throw new SecurityException("Dynamic add field method exception.", e);
        }
    }


    public SecurityContext(PolicyManager policyManager) {
        this.policyManager = policyManager;
        // init table environment
        initTableEnvironment();
        this.parser = (ParserImpl) tableEnv.getParser();
    }

    private void initTableEnvironment() {
        Configuration configuration = new Configuration();
        configuration.setString(RestOptions.BIND_PORT, "8081-8189");

        try (StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(configuration)) {
            env.setParallelism(1);

            EnvironmentSettings settings = EnvironmentSettings.newInstance()
                    .inStreamingMode()
                    .build();
            this.tableEnv = (TableEnvironmentImpl) StreamTableEnvironment.create(env, settings);

        } catch (Exception e) {
            throw new FlinkRuntimeException("Init local flink execution environment error", e);
        }
    }

    public void useCatalog(AbstractCatalog catalog) {
        if (!tableEnv.getCatalog(catalog.getName()).isPresent()) {
            tableEnv.registerCatalog(catalog.getName(), catalog);
        }
        tableEnv.useCatalog(catalog.getName());
    }

    /**
     * Add row-level filter and return new SQL
     */
    public String rewriteRowFilter(String username, String singleSql) {
        // parsing sql and return the abstract syntax tree
        SqlNode sqlNode = parser.parseSql(singleSql);

        // add row-level filter and return a new abstract syntax tree
        RowFilterVisitor visitor = new RowFilterVisitor(this, username);
        sqlNode.accept(visitor);

        return sqlNode.toString();
    }

    /**
     * Add column masking and return new SQL
     */
    public String rewriteDataMask(String username, String singleSql) {
        // parsing sql and return the abstract syntax tree
        SqlNode sqlNode = parser.parseSql(singleSql);

        // add data masking and return a new abstract syntax tree
        DataMaskVisitor visitor = new DataMaskVisitor(this, username);
        sqlNode.accept(visitor);

        return sqlNode.toString();
    }

    /**
     * Add row-level filter and column masking, then return new SQL.
     */
    public String mixedRewrite(String username, String singleSql) {
        // parsing sql and return the abstract syntax tree
        SqlNode sqlNode = parser.parseSql(singleSql);

        // add row-level filter and return a new abstract syntax tree
        RowFilterVisitor rowFilterVisitor = new RowFilterVisitor(this, username);
        sqlNode.accept(rowFilterVisitor);

        // add data masking and return a new abstract syntax tree
        DataMaskVisitor dataMaskVisitor = new DataMaskVisitor(this, username);
        sqlNode.accept(dataMaskVisitor);

        return sqlNode.toString();
    }

    /**
     * Parses a SQL expression into a {@link SqlNode}
     */
    public SqlNode parseExpression(String sqlExpression) {
        return parser.parseExpression(sqlExpression);
    }

    /**
     * Execute a SQL directly, returns 10 rows by default
     */
    public List<Row> execute(String singleSql) {
        return execute(singleSql, 10);
    }

    /**
     * Execute the single sql directly, and return size rows
     */
    public List<Row> execute(String singleSql, int size) {
        LOG.info("Execute SQL: {}", singleSql);
        TableResult tableResult = tableEnv.executeSql(singleSql);
        return fetchRows(tableResult.collect(), size);
    }

    /**
     * Execute the single sql with user rewrite policies
     */
    private List<Row> executeWithRewrite(String username, String originSql, BinaryOperator<String> rewriteFunction, int size) {
        LOG.info("Origin SQL: {}", originSql);
        String rewriteSql = rewriteFunction.apply(username, originSql);
        LOG.info("Rewrite SQL: {}", rewriteSql);
        return execute(rewriteSql, size);
    }

    /**
     * Execute the single sql with user row-level filter policies
     */
    public List<Row> executeRowFilter(String username, String singleSql, int size) {
        return executeWithRewrite(username, singleSql, this::rewriteRowFilter, size);
    }

    /**
     * Execute the single sql with user data mask policies
     */
    public List<Row> executeDataMask(String username, String singleSql, int size) {
        return executeWithRewrite(username, singleSql, this::rewriteDataMask, size);
    }

    /**
     * Execute the single sql with user row-level filter and data mask policies
     */
    public List<Row> mixedExecute(String username, String singleSql, int size) {
        return executeWithRewrite(username, singleSql, this::mixedRewrite, size);
    }

    private List<Row> fetchRows(Iterator<Row> iter, int size) {
        List<Row> rowList = new ArrayList<>(size);
        while (size > 0 && iter.hasNext()) {
            rowList.add(iter.next());
            size--;
        }
        return rowList;
    }


    private Catalog getCatalog(String catalogName) {
        return tableEnv.getCatalog(catalogName).orElseThrow(() ->
                new ValidationException(String.format("Catalog %s does not exist", catalogName))
        );
    }

    public TableEntity getTable(String tableName) {
        return getTable(tableEnv.getCurrentCatalog(), tableEnv.getCurrentDatabase(), tableName);
    }


    public String getCurrentCatalog() {
        return tableEnv.getCurrentCatalog();
    }

    public String getCurrentDatabase() {
        return tableEnv.getCurrentDatabase();
    }

    public PolicyManager getPolicyManager() {
        return policyManager;
    }


    public TableEntity getTable(String database, String tableName) {
        return getTable(tableEnv.getCurrentCatalog(), database, tableName);
    }

    public TableEntity getTable(String catalogName, String database, String tableName) {
        ObjectPath objectPath = new ObjectPath(database, tableName);
        try {
            CatalogBaseTable table = getCatalog(catalogName).getTable(objectPath);
            Schema schema = table.getUnresolvedSchema();
            LOG.info("table.schema: {}", schema);

            List<ColumnEntity> columnList = schema.getColumns()
                    .stream()
                    .map(column -> new ColumnEntity(column.getName(), processColumnType(column)))
                    .collect(Collectors.toList());

            return new TableEntity(tableName, columnList);
        } catch (TableNotExistException e) {
            throw new TableException(String.format(
                    "Cannot find table '%s' in the database %s of catalog %s .", tableName, database, catalogName));
        }
    }

    private String processColumnType(UnresolvedColumn column) {
        if (column instanceof UnresolvedComputedColumn) {
            return ((UnresolvedComputedColumn) column)
                    .getExpression()
                    .asSummaryString();
        } else if (column instanceof UnresolvedPhysicalColumn) {
            return ((UnresolvedPhysicalColumn) column).getDataType()
                    .toString()
                    // delete NOT NULL
                    .replace("NOT NULL", "")
                    .trim();
        } else if (column instanceof UnresolvedMetadataColumn) {
            return ((UnresolvedMetadataColumn) column).getDataType().toString();
        } else {
            throw new IllegalArgumentException("Unsupported column type: " + column);
        }
    }
}
