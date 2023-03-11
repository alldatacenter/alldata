package com.hw.lineage.server.infrastructure.graph;

import com.hw.lineage.common.result.ColumnResult;
import com.hw.lineage.server.domain.entity.task.Task;
import com.hw.lineage.server.domain.entity.task.TaskLineage;
import com.hw.lineage.server.domain.facade.LineageFacade;
import com.hw.lineage.server.domain.graph.GraphHelper;
import com.hw.lineage.server.domain.graph.column.ColumnEdge;
import com.hw.lineage.server.domain.graph.column.ColumnGraph;
import com.hw.lineage.server.domain.graph.column.ColumnNode;
import com.hw.lineage.server.domain.graph.table.TableEdge;
import com.hw.lineage.server.domain.graph.table.TableGraph;
import com.hw.lineage.server.domain.graph.table.TableNode;
import com.hw.lineage.server.domain.vo.SqlId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hw.lineage.common.util.Constant.DELIMITER;


/**
 * @description: GraphFactory
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class GraphFactory {

    private final AtomicInteger atomic = new AtomicInteger(1);

    private final LineageFacade lineageFacade;

    private final TableGraph tableGraph;

    private final ColumnGraph columnGraph;

    private final Map<SqlId, String> sqlSourceMap;

    public GraphFactory(LineageFacade lineageFacade, Map<SqlId, String> sqlSourceMap) {
        this.lineageFacade = lineageFacade;
        this.sqlSourceMap = sqlSourceMap;
        this.tableGraph = new TableGraph();
        this.columnGraph = new ColumnGraph();
    }

    public void createLineageGraph(String pluginCode, Task task) throws Exception {
        for (TaskLineage lineage : task.getTaskLineageList()) {
            TableNode sourceTableNode = getSourceTableNode(pluginCode, lineage);
            TableNode targetTableNode = getTargetTableNode(pluginCode, lineage);
            String sqlSource = sqlSourceMap.get(lineage.getSqlId());
            tableGraph.addEdge(new TableEdge(atomic.getAndIncrement(), sourceTableNode, targetTableNode, sqlSource));

            ColumnNode sourceColumnNode = columnGraph.queryNode(lineage.buildSourceColumnName());
            ColumnNode targetColumnNode = columnGraph.queryNode(lineage.buildTargetColumnName());
            columnGraph.addEdge(new ColumnEdge(atomic.getAndIncrement(), sourceColumnNode, targetColumnNode, lineage.getTransform()));
        }

        GraphHelper<TableNode, TableEdge> tableHelper = new GraphHelper<>(tableGraph);
        tableHelper.computeChildrenCnt();

        GraphHelper<ColumnNode, ColumnEdge> columnHelper = new GraphHelper<>(columnGraph);
        columnHelper.computeChildrenCnt();

        task.setTableGraph(tableGraph);
        task.setColumnGraph(columnGraph);
    }

    private TableNode getSourceTableNode(String pluginCode, TaskLineage lineage) throws Exception {
        String sourceTableName = lineage.buildSourceTableName();
        TableNode sourceTableNode = tableGraph.queryNode(sourceTableName);

        if (sourceTableNode == null) {
            Integer tableNodeId = atomic.getAndIncrement();
            sourceTableNode = new TableNode(tableNodeId, sourceTableName);
            tableGraph.addNode(sourceTableName, sourceTableNode);
            List<ColumnResult> columnList = lineageFacade.getTable(pluginCode, lineage.getSourceCatalog()
                    , lineage.getSourceDatabase(), lineage.getSourceTable()).getColumnList();

            for (ColumnResult column : columnList) {
                String nodeName = String.join(DELIMITER, sourceTableName, column.getColumnName());
                ColumnNode columnNode = new ColumnNode(atomic.getAndIncrement(), column.getColumnName(), tableNodeId);
                columnGraph.addNode(nodeName, columnNode);
                sourceTableNode.addColumnNode(columnNode);
            }
        }
        return sourceTableNode;
    }


    private TableNode getTargetTableNode(String pluginCode, TaskLineage lineage) throws Exception {
        String targetTableName = lineage.buildTargetTableName();
        TableNode targetTableNode = tableGraph.queryNode(targetTableName);

        if (targetTableNode == null) {
            Integer tableNodeId = atomic.getAndIncrement();
            targetTableNode = new TableNode(tableNodeId, targetTableName);
            tableGraph.addNode(targetTableName, targetTableNode);
            List<ColumnResult> columnList = lineageFacade.getTable(pluginCode, lineage.getTargetCatalog()
                    , lineage.getTargetDatabase(), lineage.getTargetTable()).getColumnList();

            for (ColumnResult column : columnList) {
                String nodeName = String.join(DELIMITER, targetTableName, column.getColumnName());
                ColumnNode columnNode = new ColumnNode(atomic.getAndIncrement(), column.getColumnName(), tableNodeId);
                columnGraph.addNode(nodeName, columnNode);
                targetTableNode.addColumnNode(columnNode);
            }

        }
        return targetTableNode;
    }
}
