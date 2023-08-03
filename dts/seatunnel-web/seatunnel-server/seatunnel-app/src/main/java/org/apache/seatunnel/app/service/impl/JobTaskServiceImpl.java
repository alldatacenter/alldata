/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seatunnel.app.service.impl;

import org.apache.seatunnel.shade.com.typesafe.config.ConfigFactory;

import org.apache.seatunnel.app.config.ConnectorDataSourceMapperConfig;
import org.apache.seatunnel.app.dal.dao.IJobLineDao;
import org.apache.seatunnel.app.dal.dao.IJobTaskDao;
import org.apache.seatunnel.app.dal.dao.IJobVersionDao;
import org.apache.seatunnel.app.dal.entity.JobLine;
import org.apache.seatunnel.app.dal.entity.JobTask;
import org.apache.seatunnel.app.dal.entity.JobVersion;
import org.apache.seatunnel.app.domain.request.connector.SceneMode;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.DatabaseTableSchemaReq;
import org.apache.seatunnel.app.domain.request.job.Edge;
import org.apache.seatunnel.app.domain.request.job.JobDAG;
import org.apache.seatunnel.app.domain.request.job.JobTaskInfo;
import org.apache.seatunnel.app.domain.request.job.PluginConfig;
import org.apache.seatunnel.app.domain.request.job.SelectTableFields;
import org.apache.seatunnel.app.domain.request.job.transform.Copy;
import org.apache.seatunnel.app.domain.request.job.transform.CopyTransformOptions;
import org.apache.seatunnel.app.domain.request.job.transform.FieldMapperTransformOptions;
import org.apache.seatunnel.app.domain.request.job.transform.RenameField;
import org.apache.seatunnel.app.domain.request.job.transform.SQLTransformOptions;
import org.apache.seatunnel.app.domain.request.job.transform.SplitTransformOptions;
import org.apache.seatunnel.app.domain.request.job.transform.Transform;
import org.apache.seatunnel.app.domain.request.job.transform.TransformOption;
import org.apache.seatunnel.app.domain.request.job.transform.TransformOptions;
import org.apache.seatunnel.app.domain.response.job.JobTaskCheckRes;
import org.apache.seatunnel.app.domain.response.job.SchemaError;
import org.apache.seatunnel.app.domain.response.job.SchemaErrorType;
import org.apache.seatunnel.app.permission.constants.SeatunnelFuncPermissionKeyConstant;
import org.apache.seatunnel.app.service.IDatasourceService;
import org.apache.seatunnel.app.service.IJobInstanceService;
import org.apache.seatunnel.app.service.IJobTaskService;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.common.utils.ExceptionUtils;
import org.apache.seatunnel.common.utils.SeaTunnelException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;
import org.apache.seatunnel.server.common.CodeGenerateUtils;
import org.apache.seatunnel.server.common.SeatunnelErrorEnum;
import org.apache.seatunnel.server.common.SeatunnelException;

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.seatunnel.app.utils.TaskOptionUtils.getTransformOption;

@Service
@Slf4j
public class JobTaskServiceImpl extends SeatunnelBaseServiceImpl implements IJobTaskService {

    @Resource(name = "jobLineDaoImpl")
    private IJobLineDao jobLineDao;

    @Resource(name = "jobTaskDaoImpl")
    private IJobTaskDao jobTaskDao;

    @Resource(name = "jobVersionDaoImpl")
    private IJobVersionDao jobVersionDao;

    @Resource private IDatasourceService datasourceService;

    @Resource private IJobInstanceService jobInstanceService;

    @Resource private ConnectorDataSourceMapperConfig connectorDataSourceMapperConfig;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private void checkConfigIntegrity(JobVersion version, JobTaskInfo jobTaskInfo) {
        if (StringUtils.isEmpty(version.getEnv())) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.ERROR_CONFIG,
                    "job env can't be empty, please change config");
        }
        Map<String, PluginConfig> pluginConfigMap =
                jobTaskInfo.getPlugins().stream()
                        .collect(Collectors.toMap(PluginConfig::getPluginId, Function.identity()));

        List<String> allPluginIdsFromEdge =
                Stream.concat(
                                jobTaskInfo.getEdges().stream().map(Edge::getInputPluginId),
                                jobTaskInfo.getEdges().stream().map(Edge::getTargetPluginId))
                        .collect(Collectors.toList());

        jobTaskInfo.getPlugins().stream()
                .filter(p -> !allPluginIdsFromEdge.contains(p.getPluginId()))
                .findAny()
                .ifPresent(
                        p -> {
                            throw new SeatunnelException(
                                    SeatunnelErrorEnum.ERROR_CONFIG,
                                    "plugin '" + p.getName() + "' is not used in any edge");
                        });

        jobTaskInfo
                .getEdges()
                .forEach(
                        e -> {
                            jobTaskInfo.getPlugins().stream()
                                    .filter(
                                            p ->
                                                    Objects.equals(
                                                            p.getPluginId(), e.getInputPluginId()))
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new SeatunnelException(
                                                            SeatunnelErrorEnum.ERROR_CONFIG,
                                                            "input plugin not found"));
                            jobTaskInfo.getPlugins().stream()
                                    .filter(
                                            p ->
                                                    Objects.equals(
                                                            p.getPluginId(), e.getTargetPluginId()))
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new SeatunnelException(
                                                            SeatunnelErrorEnum.ERROR_CONFIG,
                                                            "target plugin not found"));
                        });

        List<String> inputTransformId =
                jobTaskInfo.getEdges().stream()
                        .map(Edge::getInputPluginId)
                        .filter(e -> pluginConfigMap.get(e).getType().equals(PluginType.TRANSFORM))
                        .collect(Collectors.toList());
        List<String> targetTransformId =
                jobTaskInfo.getEdges().stream()
                        .map(Edge::getTargetPluginId)
                        .filter(e -> pluginConfigMap.get(e).getType().equals(PluginType.TRANSFORM))
                        .collect(Collectors.toList());

        if (!new HashSet<>(inputTransformId).containsAll(targetTransformId)
                || !new HashSet<>(targetTransformId).containsAll(inputTransformId)) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.ERROR_CONFIG, "transform plugin must be connected");
        }

        for (Edge edge : jobTaskInfo.getEdges()) {
            if (!pluginTypeMatch(
                    pluginConfigMap.get(edge.getInputPluginId()).getType(),
                    pluginConfigMap.get(edge.getTargetPluginId()).getType())) {
                throw new SeatunnelException(
                        SeatunnelErrorEnum.ERROR_CONFIG,
                        "plugin line not match, please check plugin line");
            }
        }
    }

    private boolean pluginTypeMatch(PluginType inputType, PluginType outputType) {
        if (inputType == PluginType.SOURCE) {
            return outputType != PluginType.SOURCE;
        }
        if (inputType == PluginType.SINK) {
            return false;
        }
        if (inputType == PluginType.TRANSFORM) {
            return outputType != PluginType.SOURCE;
        }
        return false;
    }

    private static void checkConfigFormat(String config) {
        if (StringUtils.isNotEmpty(config)) {
            ConfigFactory.parseString(config);
        }
    }

    @Override
    public JobTaskInfo getTaskConfig(long jobVersionId) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.JOB_TASK_DETAIL, 0);
        List<JobTask> tasks = jobTaskDao.getTasksByVersionId(jobVersionId);
        if (tasks.isEmpty()) {
            return null;
        }
        List<JobLine> lines = jobLineDao.getLinesByVersionId(jobVersionId);
        return new JobTaskInfo(
                lines.stream()
                        .map(l -> new Edge(l.getInputPluginId(), l.getTargetPluginId()))
                        .collect(Collectors.toList()),
                tasks.stream()
                        .map(
                                t -> {
                                    try {
                                        return getPluginConfigFromJobTask(t);
                                    } catch (Exception e) {
                                        throw new SeatunnelException(
                                                SeatunnelErrorEnum.UNKNOWN, e.getMessage());
                                    }
                                })
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public JobTaskCheckRes saveJobDAG(long jobVersionId, JobDAG jobDAG) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.JOB_TASK_DAG_CREATE, 0);
        JobVersion version = jobVersionDao.getVersionById(jobVersionId);
        List<JobTask> tasks = jobTaskDao.getTasksByVersionId(jobVersionId);
        List<PluginConfig> pluginConfigs =
                tasks.stream()
                        .map(JobTaskServiceImpl::getPluginConfigFromJobTask)
                        .collect(Collectors.toList());
        JobTaskInfo taskInfo = new JobTaskInfo(jobDAG.getEdges(), pluginConfigs);
        checkConfigIntegrity(version, taskInfo);
        List<JobLine> lines =
                jobDAG.getEdges().stream()
                        .map(
                                e -> {
                                    try {
                                        return JobLine.builder()
                                                .id(CodeGenerateUtils.getInstance().genCode())
                                                .inputPluginId(e.getInputPluginId())
                                                .targetPluginId(e.getTargetPluginId())
                                                .versionId(jobVersionId)
                                                .build();
                                    } catch (CodeGenerateUtils.CodeGenerateException ex) {
                                        throw new SeatunnelException(
                                                SeatunnelErrorEnum.ILLEGAL_STATE, ex.getMessage());
                                    }
                                })
                        .collect(Collectors.toList());

        try {
            JobTaskCheckRes jobTaskCheckRes = checkPluginSchemaIntegrity(taskInfo);
            if (jobTaskCheckRes != null) {
                return jobTaskCheckRes;
            }
            // check the config can be generated
            jobInstanceService.generateJobConfig(
                    version.getJobId(), tasks, lines, version.getEnv());
            // TODO check schema output and input matched
        } catch (SeaTunnelException e) {
            log.error(ExceptionUtils.getMessage(e));
            throw e;
        } catch (Exception e) {
            throw new SeatunnelException(SeatunnelErrorEnum.ERROR_CONFIG, e.getMessage());
        }

        jobLineDao.deleteLinesByVersionId(jobVersionId);
        jobLineDao.insertLines(lines);
        return null;
    }

    private JobTaskCheckRes checkPluginSchemaIntegrity(JobTaskInfo taskInfo) throws IOException {

        List<PluginConfig> source =
                taskInfo.getPlugins().stream()
                        .filter(p -> p.getType().equals(PluginType.SOURCE))
                        .collect(Collectors.toList());

        Map<String, PluginConfig> pluginMap =
                taskInfo.getPlugins().stream()
                        .collect(Collectors.toMap(PluginConfig::getPluginId, Function.identity()));
        Map<String, String> edgeMap =
                taskInfo.getEdges().stream()
                        .collect(Collectors.toMap(Edge::getInputPluginId, Edge::getTargetPluginId));

        for (PluginConfig config : source) {
            PluginConfig nextConfig = pluginMap.get(edgeMap.get(config.getPluginId()));
            JobTaskCheckRes res = checkNextTaskSchema(config, nextConfig, pluginMap, edgeMap);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    private JobTaskCheckRes checkNextTaskSchema(
            PluginConfig config,
            PluginConfig nextConfig,
            Map<String, PluginConfig> pluginMap,
            Map<String, String> edgeMap)
            throws IOException {
        Map<String, Object> options = nextConfig.getTransformOptions();
        if (options != null && !options.isEmpty()) {
            Transform transform = Transform.valueOf(nextConfig.getConnectorType().toUpperCase());
            String transformOptionsStr = OBJECT_MAPPER.writeValueAsString(options);

            List<TransformOption> transformOptions = new ArrayList<>();

            switch (transform) {
                case FIELDMAPPER:
                    FieldMapperTransformOptions fieldMapperTransformOptions =
                            getTransformOption(transform, transformOptionsStr);
                    if (fieldMapperTransformOptions != null) {
                        fillTransformOptions(
                                transformOptions, fieldMapperTransformOptions.getRenameFields());
                        fillTransformOptions(
                                transformOptions, fieldMapperTransformOptions.getChangeOrders());
                    }
                    break;
                case MULTIFIELDSPLIT:
                    SplitTransformOptions splitTransformOptions =
                            getTransformOption(transform, transformOptionsStr);
                    if (splitTransformOptions != null) {
                        fillTransformOptions(transformOptions, splitTransformOptions.getSplits());
                    }
                    break;
                case COPY:
                    CopyTransformOptions copyTransformOptions =
                            getTransformOption(transform, transformOptionsStr);
                    if (copyTransformOptions != null) {
                        fillTransformOptions(transformOptions, copyTransformOptions.getCopyList());
                    }
                    break;
                case SQL:
                    SQLTransformOptions sqlTransformOptions =
                            getTransformOption(transform, transformOptionsStr);
                    if (sqlTransformOptions != null) {
                        fillTransformOptions(
                                transformOptions,
                                Collections.singletonList(sqlTransformOptions.getSql()));
                    }
                    break;
                case FILTERROWKIND:
                case REPLACE:
                default:
                    throw new SeatunnelException(
                            SeatunnelErrorEnum.UNSUPPORTED_CONNECTOR_TYPE,
                            "Unsupported Transform Option " + transform);
            }

            if (!transformOptions.isEmpty()) {
                DatabaseTableSchemaReq databaseTableSchemaReq = config.getOutputSchema().get(0);
                List<String> fields =
                        databaseTableSchemaReq.getFields().stream()
                                .map(TableField::getName)
                                .collect(Collectors.toList());
                Optional<TransformOption> transformOption =
                        transformOptions.stream()
                                .filter(option -> !fields.contains(option.getSourceFieldName()))
                                .findFirst();
                if (transformOption.isPresent()) {
                    String sourceFiledName = transformOption.get().getSourceFieldName();
                    return new JobTaskCheckRes(
                            false,
                            nextConfig.getPluginId(),
                            new SchemaError(
                                    databaseTableSchemaReq.getDatabase(),
                                    databaseTableSchemaReq.getTableName(),
                                    sourceFiledName,
                                    SchemaErrorType.MISS_FIELD),
                            null);
                }
            }
        }
        if (edgeMap.containsKey(nextConfig.getPluginId())) {
            return checkNextTaskSchema(
                    nextConfig,
                    pluginMap.get(edgeMap.get(nextConfig.getPluginId())),
                    pluginMap,
                    edgeMap);
        }
        return null;
    }

    private static void fillTransformOptions(
            List<TransformOption> transformOptions, List<? extends TransformOption> options) {
        if (options != null) {
            transformOptions.addAll(options);
        }
    }

    @Override
    public void saveSingleTask(long jobVersionId, PluginConfig pluginConfig) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.SINGLE_TASK_CREATE, 0);
        JobTask jobTask;
        JobTask old = jobTaskDao.getTask(jobVersionId, pluginConfig.getPluginId());
        try {
            checkConfigFormat(pluginConfig.getConfig());
            long id;
            if (old != null) {
                id = old.getId();
            } else {
                id = CodeGenerateUtils.getInstance().genCode();
            }
            String connectorType;
            String transformOptionsStr = null;
            if (pluginConfig.getType().equals(PluginType.TRANSFORM)) {
                connectorType = pluginConfig.getConnectorType();
                if (pluginConfig.getTransformOptions() != null) {
                    transformOptionsStr =
                            OBJECT_MAPPER.writeValueAsString(pluginConfig.getTransformOptions());
                }
                transformOptionCheck(connectorType, transformOptionsStr);
            } else {
                connectorType = getConnectorTypeFromDataSource(pluginConfig.getDataSourceId());
            }
            jobTask =
                    JobTask.builder()
                            .id(id)
                            .pluginId(pluginConfig.getPluginId())
                            .name(pluginConfig.getName())
                            .type(pluginConfig.getType().name().toUpperCase())
                            .dataSourceId(pluginConfig.getDataSourceId())
                            .config(pluginConfig.getConfig())
                            .sceneMode(
                                    pluginConfig.getSceneMode() == null
                                            ? null
                                            : pluginConfig.getSceneMode().name())
                            .versionId(jobVersionId)
                            .connectorType(connectorType)
                            .dataSourceOption(
                                    pluginConfig.getTableOption() == null
                                            ? null
                                            : OBJECT_MAPPER.writeValueAsString(
                                                    pluginConfig.getTableOption()))
                            .selectTableFields(
                                    pluginConfig.getSelectTableFields() == null
                                            ? null
                                            : OBJECT_MAPPER.writeValueAsString(
                                                    pluginConfig.getSelectTableFields()))
                            .outputSchema(
                                    pluginConfig.getOutputSchema() == null
                                            ? null
                                            : OBJECT_MAPPER.writeValueAsString(
                                                    pluginConfig.getOutputSchema()))
                            .transformOptions(transformOptionsStr)
                            .build();
        } catch (Exception e) {
            throw new SeatunnelException(SeatunnelErrorEnum.ILLEGAL_STATE, e.getMessage());
        }
        if (old != null) {
            jobTaskDao.updateTask(jobTask);
        } else {
            jobTaskDao.insertTask(jobTask);
        }
    }

    private void transformOptionCheck(String connectorType, String transformOptionsStr)
            throws IOException {
        Transform transform = Transform.valueOf(connectorType.toUpperCase());
        switch (transform) {
            case FIELDMAPPER:
                FieldMapperTransformOptions fieldMapperTransformOptions =
                        getTransformOption(transform, transformOptionsStr);
                if (fieldMapperTransformOptions != null) {
                    List<RenameField> renameFields = fieldMapperTransformOptions.getRenameFields();
                    checkTransformTargetFieldRepeat(
                            renameFields.stream()
                                    .map(RenameField::getTargetName)
                                    .collect(Collectors.toList()));
                }
                break;
            case MULTIFIELDSPLIT:
                SplitTransformOptions splitTransformOptions =
                        getTransformOption(transform, transformOptionsStr);
                if (splitTransformOptions != null) {
                    List<String> fields =
                            splitTransformOptions.getSplits().stream()
                                    .flatMap(split -> split.getOutputFields().stream())
                                    .collect(Collectors.toList());
                    checkTransformTargetFieldRepeat(fields);
                }
                break;
            case COPY:
                CopyTransformOptions copyTransformOptions =
                        getTransformOption(transform, transformOptionsStr);
                if (copyTransformOptions != null) {
                    List<String> fields =
                            copyTransformOptions.getCopyList().stream()
                                    .map(Copy::getTargetFieldName)
                                    .collect(Collectors.toList());
                    checkTransformTargetFieldRepeat(fields);
                }
                break;
            case SQL:
                SQLTransformOptions sqlTransformOptions =
                        getTransformOption(transform, transformOptionsStr);
                if (sqlTransformOptions != null) {
                    // TODO 调用接口返回目标字段
                    List<String> fields = new ArrayList<>();
                    checkTransformTargetFieldRepeat(fields);
                }
                break;
            case FILTERROWKIND:
            case REPLACE:
            default:
        }
    }

    private void checkTransformTargetFieldRepeat(List<String> fields) {
        Set<String> duplicates =
                fields.stream()
                        .filter(i -> Collections.frequency(fields, i) > 1)
                        .collect(Collectors.toSet());
        if (!duplicates.isEmpty()) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.ILLEGAL_STATE,
                    "Can't convert same target name: " + new ArrayList<>(duplicates));
        }
    }

    @Override
    public PluginConfig getSingleTask(long jobVersionId, String pluginId) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.SINGLE_TASK_DETAIL, 0);
        return getPluginConfigFromJobTask(jobTaskDao.getTask(jobVersionId, pluginId));
    }

    @Override
    public <T extends TransformOptions> T getTransformOptions(long jobVersionId, String pluginId) {
        JobTask jobTask = jobTaskDao.getTask(jobVersionId, pluginId);
        try {
            Transform transform = Transform.valueOf(jobTask.getConnectorType().toUpperCase());
            return getTransformOption(transform, jobTask.getTransformOptions());
        } catch (IOException e) {
            throw new SeatunnelException(SeatunnelErrorEnum.ILLEGAL_STATE, e.getMessage());
        }
    }

    @Override
    public void deleteSingleTask(long jobVersionId, String pluginId) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.SINGLE_TASK_DELETE, 0);
        jobTaskDao.deleteTask(jobVersionId, pluginId);
    }

    private String getConnectorTypeFromDataSource(long datasourceId) {
        String pluginName =
                datasourceService
                        .queryDatasourceDetailById(String.valueOf(datasourceId))
                        .getPluginName();
        return connectorDataSourceMapperConfig
                .findConnectorForDatasourceName(pluginName)
                .orElseThrow(
                        () ->
                                new SeatunnelException(
                                        SeatunnelErrorEnum.ILLEGAL_STATE,
                                        "can not find connector for datasourceName: "
                                                + pluginName));
    }

    private static PluginConfig getPluginConfigFromJobTask(JobTask jobTask) {
        try {
            return PluginConfig.builder()
                    .pluginId(jobTask.getPluginId())
                    .name(jobTask.getName())
                    .type(PluginType.valueOf(jobTask.getType().toUpperCase()))
                    .dataSourceId(jobTask.getDataSourceId())
                    .config(jobTask.getConfig())
                    .connectorType(jobTask.getConnectorType())
                    .sceneMode(
                            StringUtils.isEmpty(jobTask.getSceneMode())
                                    ? null
                                    : SceneMode.valueOf(jobTask.getSceneMode()))
                    .tableOption(
                            StringUtils.isEmpty(jobTask.getDataSourceOption())
                                    ? null
                                    : OBJECT_MAPPER.readValue(
                                            jobTask.getDataSourceOption(), DataSourceOption.class))
                    .selectTableFields(
                            StringUtils.isEmpty(jobTask.getSelectTableFields())
                                    ? null
                                    : OBJECT_MAPPER.readValue(
                                            jobTask.getSelectTableFields(),
                                            SelectTableFields.class))
                    .outputSchema(
                            StringUtils.isEmpty(jobTask.getOutputSchema())
                                    ? null
                                    : OBJECT_MAPPER.readValue(
                                            jobTask.getOutputSchema(),
                                            new TypeReference<List<DatabaseTableSchemaReq>>() {}))
                    .transformOptions(
                            StringUtils.isEmpty(jobTask.getTransformOptions())
                                    ? null
                                    : OBJECT_MAPPER.readValue(
                                            jobTask.getTransformOptions(),
                                            new TypeReference<Map<String, Object>>() {}))
                    .config(jobTask.getConfig())
                    .build();
        } catch (Exception e) {
            throw new SeatunnelException(SeatunnelErrorEnum.UNKNOWN, e.getMessage());
        }
    }
}
