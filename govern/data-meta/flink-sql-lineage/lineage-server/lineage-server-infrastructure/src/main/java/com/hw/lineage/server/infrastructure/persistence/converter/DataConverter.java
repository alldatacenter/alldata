package com.hw.lineage.server.infrastructure.persistence.converter;

import com.hw.lineage.server.domain.entity.*;
import com.hw.lineage.server.domain.entity.task.Task;
import com.hw.lineage.server.domain.entity.task.TaskLineage;
import com.hw.lineage.server.domain.entity.task.TaskSql;
import com.hw.lineage.server.infrastructure.persistence.dos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @description: DataConverter
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Mapper(componentModel = "spring")
public interface DataConverter {
    DataConverter INSTANCE = Mappers.getMapper(DataConverter.class);

    @Mapping(source = "taskId", target = "taskId.value")
    @Mapping(source = "catalogId", target = "catalogId.value")
    @Mapping(source = "taskSource", target = "taskSource.value")
    Task toTask(TaskDO taskDO);

    @Mapping(source = "taskId.value", target = "taskId")
    @Mapping(source = "catalogId.value", target = "catalogId")
    @Mapping(source = "taskSource.value", target = "taskSource")
    TaskDO fromTask(Task task);

    List<Task> toTaskList(List<TaskDO> taskDOList);

    @Mapping(source = "taskId", target = "taskId.value")
    @Mapping(source = "sqlId", target = "sqlId.value")
    TaskSql toTaskSql(TaskSqlDO taskSqlDO);

    @Mapping(source = "taskId.value", target = "taskId")
    @Mapping(source = "sqlId.value", target = "sqlId")
    TaskSqlDO fromTaskSql(TaskSql taskSql);


    @Mapping(source = "taskId", target = "taskId.value")
    @Mapping(source = "sqlId", target = "sqlId.value")
    TaskLineage toTaskLineage(TaskLineageDO taskLineageDO);

    @Mapping(source = "taskId.value", target = "taskId")
    @Mapping(source = "sqlId.value", target = "sqlId")
    TaskLineageDO fromTaskLineage(TaskLineage taskLineage);

    @Mapping(source = "pluginId", target = "pluginId.value")
    Plugin toPlugin(PluginDO pluginDO);

    @Mapping(source = "pluginId.value", target = "pluginId")
    PluginDO fromPlugin(Plugin plugin);

    @Mapping(source = "catalogId", target = "catalogId.value")
    @Mapping(source = "pluginId", target = "pluginId.value")
    Catalog toCatalog(CatalogDO catalogDO);

    @Mapping(source = "catalogId.value", target = "catalogId")
    @Mapping(source = "pluginId.value", target = "pluginId")
    CatalogDO fromCatalog(Catalog catalog);

    @Mapping(source = "functionId", target = "functionId.value")
    @Mapping(source = "catalogId", target = "catalogId.value")
    Function toFunction(FunctionDO pluginDO);

    List<Function> toFunctionList(List<FunctionDO> functionDOList);

    @Mapping(source = "functionId.value", target = "functionId")
    @Mapping(source = "catalogId.value", target = "catalogId")
    FunctionDO fromFunction(Function function);

    @Mapping(source = "userId", target = "userId.value")
    User toUser(UserDO userDO);

    @Mapping(source = "userId.value", target = "userId")
    UserDO fromUser(User user);

    @Mapping(source = "roleId", target = "roleId.value")
    Role toRole(RoleDO roleDO);

    @Mapping(source = "roleId.value", target = "roleId")
    RoleDO fromRole(Role role);

    @Mapping(source = "permissionId", target = "permissionId.value")
    Permission toPermission(PermissionDO permissionDO);

    @Mapping(source = "permissionId.value", target = "permissionId")
    PermissionDO fromPermission(Permission permission);

    List<Permission> toPermissionList(List<PermissionDO> permissionDOList);

    List<Role> toRoleList(List<RoleDO> roleDOList);

    List<User> toUserList(List<UserDO> userDOList);

}
