package com.hw.lineage.server.application.assembler;

import com.google.common.base.Strings;
import com.hw.lineage.server.application.dto.*;
import com.hw.lineage.server.application.dto.graph.LineageGraph;
import com.hw.lineage.server.application.dto.graph.link.ColumnLink;
import com.hw.lineage.server.application.dto.graph.link.TableLink;
import com.hw.lineage.server.application.dto.graph.link.basic.Link;
import com.hw.lineage.server.application.dto.graph.vertex.Column;
import com.hw.lineage.server.application.dto.graph.vertex.Vertex;
import com.hw.lineage.server.domain.entity.*;
import com.hw.lineage.server.domain.entity.task.Task;
import com.hw.lineage.server.domain.entity.task.TaskLineage;
import com.hw.lineage.server.domain.entity.task.TaskSql;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

import static com.hw.lineage.common.util.Constant.DELIMITER;

/**
 * @description: TaskAssembler
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Mapper(componentModel = "spring")
public interface DtoAssembler {
    DtoAssembler INSTANCE = Mappers.getMapper(DtoAssembler.class);

    @Mapping(source = "taskId.value", target = "taskId")
    @Mapping(source = "catalogId.value", target = "catalogId")
    @Mapping(source = "taskSource.value", target = "taskSource")
    TaskDTO fromTask(Task task);

    @Mapping(source = "task.taskId.value", target = "taskId")
    @Mapping(source = "task.catalogId.value", target = "catalogId")
    @Mapping(source = "task.taskSource.value", target = "taskSource")
    TaskDTO fromTask(Task task, String catalogName);

    @Mapping(source = "catalogId.value", target = "catalogId")
    @Mapping(source = "pluginId.value", target = "pluginId")
    CatalogDTO fromCatalog(Catalog catalog);

    @Mapping(source = "functionId.value", target = "functionId")
    @Mapping(source = "catalogId.value", target = "catalogId")
    FunctionDTO fromFunction(Function function);

    @Mapping(source = "pluginId.value", target = "pluginId")
    PluginDTO fromPlugin(Plugin plugin);

    List<TaskDTO> fromTaskList(List<Task> taskList);

    @Mapping(source = "taskId.value", target = "taskId")
    @Mapping(source = "sqlId.value", target = "sqlId")
    TaskSqlDTO fromTaskSql(TaskSql taskSql);

    @Mapping(source = "taskId.value", target = "taskId")
    @Mapping(source = "sqlId.value", target = "sqlId")
    TaskLineageDTO fromTaskLineage(TaskLineage taskLineage);

    List<FunctionDTO> fromFunctionList(List<Function> functionList);

    @Mapping(source = "userId.value", target = "userId")
    UserDTO fromUser(User user);

    @Mapping(source = "user.userId.value", target = "userId")
    UserDTO fromUserPermissions(User user, List<Permission> permissionList);

    @Mapping(source = "user.userId.value", target = "userId")
    UserDTO fromUserRoles(User user, List<Role> roleList);


    @Mapping(source = "roleId.value", target = "roleId")
    RoleDTO fromRole(Role role);

    @Mapping(source = "permissionId.value", target = "permissionId")
    PermissionDTO fromPermission(Permission permission);

    @AfterMapping
    default void setTaskLineageGraph(@MappingTarget TaskDTO taskDTO, Task task, String catalogName) {
        List<Vertex> vertexList = task.getTableGraph().queryNodeSet()
                .stream()
                .map(tableNode -> {
                    List<Column> columnList = tableNode.getColumnNodeList()
                            .stream()
                            .map(columnNode -> new Column(columnNode.getNodeId(), columnNode.getNodeName(), columnNode.getChildrenCnt()))
                            .collect(Collectors.toList());

                    String optimizedName = optimizeName(catalogName, task.getDatabase(), tableNode.getNodeName());
                    return new Vertex().setId(tableNode.getNodeId())
                            .setName(optimizedName)
                            .setColumns(columnList)
                            .setHasUpstream(!tableNode.getParentIdSet().isEmpty())
                            .setHasDownstream(!tableNode.getChildIdSet().isEmpty())
                            .setChildrenCnt(tableNode.getChildrenCnt());
                })
                .collect(Collectors.toList());

        // add table edges
        List<Link> linkList = task.getTableGraph().getEdgeSet()
                .stream()
                .map(tableEdge -> new TableLink(tableEdge.getEdgeId()
                        , tableEdge.getSource().getNodeId()
                        , tableEdge.getTarget().getNodeId()
                        , tableEdge.getSqlSource()))
                .collect(Collectors.toList());

        // add column edges
        linkList.addAll(task.getColumnGraph().getEdgeSet()
                .stream()
                .map(columnEdge -> new ColumnLink(columnEdge.getEdgeId()
                        , columnEdge.getSource().getTableNodeId()
                        , columnEdge.getTarget().getTableNodeId()
                        , columnEdge.getSource().getNodeId()
                        , columnEdge.getTarget().getNodeId()
                        , Strings.nullToEmpty(columnEdge.getTransform())
                ))
                .collect(Collectors.toList())
        );
        LineageGraph lineageGraph = new LineageGraph().setNodes(vertexList).setLinks(linkList);
        taskDTO.setLineageGraph(lineageGraph);
    }


    default String optimizeName(String catalogName, String database, String tableName) {
        if (tableName.startsWith(catalogName)) {
            tableName = tableName.replaceFirst(catalogName + DELIMITER, "");
        }
        if (tableName.startsWith(database)) {
            tableName = tableName.replaceFirst(database + DELIMITER, "");
        }
        return tableName;
    }
}
